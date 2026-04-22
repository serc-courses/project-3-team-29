package com.iiit.oms.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.Order;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Publishes order lifecycle events to Kafka topics.
 * Purely additive: if Kafka is unavailable, events are silently dropped and
 * the core OMS flow continues unaffected.
 *
 * Topics:
 *  - oms.orders.planned    – new order received
 *  - oms.orders.state      – any order status change
 *  - oms.bulk.created      – new bulk order created
 *  - oms.bulk.transmitted  – bulk order transmitted to TA
 *  - oms.orders.booked     – order reached terminal BOOKED state
 */
public class KafkaOrderEventPublisher {
    private static final Logger LOGGER = Logger.getLogger(KafkaOrderEventPublisher.class.getName());

    public static final String TOPIC_ORDER_PLANNED    = "oms.orders.planned";
    public static final String TOPIC_ORDER_STATE      = "oms.orders.state";
    public static final String TOPIC_BULK_CREATED     = "oms.bulk.created";
    public static final String TOPIC_BULK_TRANSMITTED = "oms.bulk.transmitted";
    public static final String TOPIC_ORDER_BOOKED     = "oms.orders.booked";
    public static final String TOPIC_ORDER_RECEIVED   = "oms.orders.received";

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public KafkaOrderEventPublisher(String bootstrapServers) {
        this.objectMapper = new ObjectMapper();
        KafkaProducer<String, String> p = null;
        boolean ok = false;
        try {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "1");
            props.put(ProducerConfig.RETRIES_CONFIG, 0);
            props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000); // 2s metadata fetch timeout
            p = new KafkaProducer<>(props);
            ok = true;
            LOGGER.info("Kafka producer initialized at " + bootstrapServers);
        } catch (Exception ex) {
            LOGGER.warning("Kafka not available at " + bootstrapServers + " – event publishing disabled: " + ex.getMessage());
        }
        this.producer = p;
        this.enabled = ok;
    }

    public void publishOrderPlanned(Order order) {
        publish(TOPIC_ORDER_PLANNED, order.getOrderID(), orderEvent("ORDER_PLANNED", order));
    }

    public void publishOrderStateChanged(Order order, String fromStatus) {
        Map<String, Object> payload = orderEvent("ORDER_STATE_CHANGED", order);
        payload.put("fromStatus", fromStatus);
        publish(TOPIC_ORDER_STATE, order.getOrderID(), payload);
        if ("BOOKED".equals(order.getOrderStatus().name())) {
            publish(TOPIC_ORDER_BOOKED, order.getOrderID(), payload);
        }
    }

    public void publishBulkOrderCreated(BulkOrder bulkOrder) {
        publish(TOPIC_BULK_CREATED, bulkOrder.getOrderID(), bulkEvent("BULK_ORDER_CREATED", bulkOrder));
    }

    public void publishBulkOrderTransmitted(BulkOrder bulkOrder) {
        publish(TOPIC_BULK_TRANSMITTED, bulkOrder.getOrderID(), bulkEvent("BULK_ORDER_TRANSMITTED", bulkOrder));
    }

    /**
     * Publish an order to the oms.orders.received topic for async processing by
     * KafkaOrderProcessingConsumer.
     */
    public void publishOrderReceived(Order order) {
        Map<String, Object> m = new HashMap<>();
        m.put("eventType", "ORDER_RECEIVED");
        m.put("orderID", order.getOrderID());
        m.put("productID", order.getProductID());
        m.put("accountID", order.getAccountID());
        m.put("amount", order.getAmount());
        m.put("orderSide", order.getOrderSide().name());
        m.put("orderStatus", order.getOrderStatus().name());
        m.put("timestamp", Instant.now().toString());
        publish(TOPIC_ORDER_RECEIVED, order.getOrderID(), m);
    }

    private Map<String, Object> orderEvent(String eventType, Order order) {
        Map<String, Object> m = new HashMap<>();
        m.put("eventType", eventType);
        m.put("orderID", order.getOrderID());
        m.put("productID", order.getProductID());
        m.put("accountID", order.getAccountID());
        m.put("amount", order.getAmount());
        m.put("orderSide", order.getOrderSide().name());
        m.put("orderStatus", order.getOrderStatus().name());
        m.put("transferAgent", order.getTransferAgent());
        m.put("timestamp", Instant.now().toString());
        return m;
    }

    private Map<String, Object> bulkEvent(String eventType, BulkOrder bulkOrder) {
        Map<String, Object> m = new HashMap<>();
        m.put("eventType", eventType);
        m.put("bulkOrderID", bulkOrder.getOrderID());
        m.put("productID", bulkOrder.getProductID());
        m.put("orderSide", bulkOrder.getOrderSide().name());
        m.put("status", bulkOrder.getBulkOrderStatus().name());
        m.put("transferAgent", bulkOrder.getTransferAgent());
        m.put("transmissionRef", bulkOrder.getTransmissionRef());
        m.put("timestamp", Instant.now().toString());
        return m;
    }

    private void publish(String topic, String key, Map<String, Object> payload) {
        if (!enabled || producer == null) return;
        try {
            String value = objectMapper.writeValueAsString(payload);
            producer.send(new ProducerRecord<>(topic, key, value), (meta, ex) -> {
                if (ex != null) {
                    LOGGER.warning("Failed to publish to Kafka topic " + topic + ": " + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            LOGGER.warning("Kafka publish error: " + ex.getMessage());
        }
    }

    public boolean isEnabled() { return enabled; }

    public void close() {
        if (producer != null) {
            try { producer.close(); } catch (Exception ignored) { }
        }
    }
}
