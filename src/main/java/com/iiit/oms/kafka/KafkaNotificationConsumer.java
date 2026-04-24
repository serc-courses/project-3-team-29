package com.iiit.oms.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiit.oms.interfaces.SseBroadcaster;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Kafka consumer that subscribes to oms.orders.booked (and oms.orders.state) topics
 * and bridges events to connected SSE clients for real-time notifications.
 * Runs in a daemon thread. Silently disabled if Kafka is unavailable.
 */
public class KafkaNotificationConsumer implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(KafkaNotificationConsumer.class.getName());
    // Each JVM instance needs a UNIQUE consumer group so that both replicas receive
    // every Kafka message and can forward it to their locally-connected SSE clients.
    // A shared group would deliver each message to only one of the two replicas,
    // causing the SSE client on the other replica to miss the event.
    private static final String GROUP_ID = "oms-sse-" + java.util.UUID.randomUUID().toString().substring(0, 8);

    private final SseBroadcaster sseBroadcaster;
    private final String bootstrapServers;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean running = true;
    private boolean enabled = false;

    public KafkaNotificationConsumer(String bootstrapServers, SseBroadcaster sseBroadcaster) {
        this.bootstrapServers = bootstrapServers;
        this.sseBroadcaster = sseBroadcaster;
    }

    public void start() {
        Thread thread = new Thread(this, "kafka-notification-consumer");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void run() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(
                    KafkaOrderEventPublisher.TOPIC_ORDER_BOOKED,
                    KafkaOrderEventPublisher.TOPIC_ORDER_STATE
            ));
            enabled = true;
            LOGGER.info("KafkaNotificationConsumer started, subscribed to [oms.orders.booked, oms.orders.state]");

            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    for (ConsumerRecord<String, String> record : records) {
                        handleRecord(record);
                    }
                } catch (Exception ex) {
                    if (running) {
                        LOGGER.warning("Kafka poll error: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("KafkaNotificationConsumer failed to start: " + ex.getMessage());
            enabled = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRecord(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("eventType");
            LOGGER.info("KafkaNotificationConsumer received event: " + eventType + " key=" + record.key() + " topic=" + record.topic());

            // Bridge Kafka event to SSE stream for real-time UI updates
            if (sseBroadcaster != null) {
                String sseEventType = "order-updated";
                if (record.topic().contains("bulk") || "BULK_ORDER_CREATED".equals(eventType)
                        || "BULK_ORDER_TRANSMITTED".equals(eventType)) {
                    sseEventType = "bulk-order-updated";
                }
                sseBroadcaster.broadcastRawEvent(sseEventType, record.value());
            }
        } catch (Exception ex) {
            LOGGER.warning("Failed to handle Kafka notification record: " + ex.getMessage());
        }
    }
}
