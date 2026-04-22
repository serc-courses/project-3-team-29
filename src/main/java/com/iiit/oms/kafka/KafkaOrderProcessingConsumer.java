package com.iiit.oms.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiit.oms.interfaces.SseBroadcaster;
import com.iiit.oms.model.Fund;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.processor.OrderScheduler;
import com.iiit.oms.processor.OrderStateMachine;
import com.iiit.oms.readmodel.OrderProjectionListener;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Kafka Problem 1: Async order processing consumer.
 *
 * Subscribes to {@code oms.orders.received}. For each message, it runs the
 * full validate → enrich → place pipeline via {@link OrderStateMachine}, then
 * saves the order and updates the projection store.
 *
 * This allows the HTTP handler to return 202 Accepted immediately after
 * publishing to Kafka, making order intake fully asynchronous.
 */
public class KafkaOrderProcessingConsumer implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(KafkaOrderProcessingConsumer.class.getName());
    private static final String GROUP_ID = "oms-order-processing";

    private final String bootstrapServers;
    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final FundRepository fundRepository;
    private final OrderProjectionListener projectionListener;
    private final SseBroadcaster sseBroadcaster;
    private final KafkaOrderEventPublisher kafkaPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean running = true;
    private boolean enabled = false;

    public KafkaOrderProcessingConsumer(
            String bootstrapServers,
            OrderRepository orderRepository,
            OrderStateMachine orderStateMachine,
            FundRepository fundRepository,
            OrderProjectionListener projectionListener,
            SseBroadcaster sseBroadcaster,
            KafkaOrderEventPublisher kafkaPublisher) {
        this.bootstrapServers = bootstrapServers;
        this.orderRepository = orderRepository;
        this.orderStateMachine = orderStateMachine;
        this.fundRepository = fundRepository;
        this.projectionListener = projectionListener;
        this.sseBroadcaster = sseBroadcaster;
        this.kafkaPublisher = kafkaPublisher;
    }

    public void start() {
        Thread t = new Thread(this, "kafka-order-processing-consumer");
        t.setDaemon(true);
        t.start();
    }

    public void stop() { running = false; }
    public boolean isEnabled() { return enabled; }

    @Override
    public void run() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "60000");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(KafkaOrderEventPublisher.TOPIC_ORDER_RECEIVED));
            enabled = true;
            LOGGER.info("KafkaOrderProcessingConsumer started, subscribed to [oms.orders.received]");

            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    for (ConsumerRecord<String, String> record : records) {
                        handleRecord(record);
                    }
                } catch (Exception ex) {
                    if (running) {
                        LOGGER.warning("Kafka order processing poll error: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("KafkaOrderProcessingConsumer failed to start: " + ex.getMessage());
            enabled = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRecord(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(),
                    new TypeReference<Map<String, Object>>() {});

            String orderID = (String) event.get("orderID");
            if (orderID == null) {
                LOGGER.warning("Received order event without orderID – skipping");
                return;
            }

            // Check if already processed (idempotency: skip if order already in DB beyond PLANNED)
            Optional<Order> existing = orderRepository.findByOrderId(orderID);
            if (existing.isPresent() && existing.get().isProcessed()) {
                LOGGER.fine("Order " + orderID + " already processed – skipping Kafka duplicate");
                return;
            }

            Order order;
            if (existing.isPresent()) {
                order = existing.get();
            } else {
                // Reconstruct order from Kafka message
                order = new Order();
                order.setOrderID(orderID);
                order.setProductID((String) event.get("productID"));
                order.setAccountID((String) event.get("accountID"));
                String amountStr = event.get("amount") != null ? event.get("amount").toString() : "0";
                order.setAmount(new BigDecimal(amountStr));
                String side = (String) event.getOrDefault("orderSide", "BUY");
                order.setOrderSide(OrderSide.valueOf(side));
                order.setOrderStatus(OrderStatus.PLANNED);
                order.setProcessed(false);
                orderRepository.save(order);
            }

            // Run the state machine: PLANNED → VALIDATED → ENRICHED → PLACED
            OrderStatus before = order.getOrderStatus();
            Order processed = orderStateMachine.process(order);
            processed.setProcessed(true);
            orderRepository.save(processed);

            // Update projection
            if (projectionListener != null && fundRepository != null) {
                Optional<Fund> maybeFund = fundRepository.findByFundId(processed.getProductID());
                if (maybeFund.isPresent()) {
                    projectionListener.onOrderStatusChanged(processed, null, maybeFund.get());
                }
            }

            // SSE notification
            if (sseBroadcaster != null) {
                sseBroadcaster.broadcastOrderUpdate(processed, null);
            }

            // Kafka downstream event
            if (kafkaPublisher != null && kafkaPublisher.isEnabled()) {
                kafkaPublisher.publishOrderStateChanged(processed, before.name());
            }

            LOGGER.info("KafkaOrderProcessingConsumer: processed order " + orderID
                    + " → " + processed.getOrderStatus());
        } catch (Exception ex) {
            LOGGER.warning("KafkaOrderProcessingConsumer: failed to process record key="
                    + record.key() + ": " + ex.getMessage());
        }
    }
}
