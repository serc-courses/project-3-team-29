package com.iiit.oms.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiit.oms.model.AuditLogEntry;
import com.iiit.oms.repository.AuditLogRepository;
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
 * Kafka consumer that subscribes to ALL OMS topics and writes events as
 * audit log entries. This decouples audit logging from the processing pipeline,
 * ensuring every event published to Kafka is captured regardless of which
 * component generated it.
 *
 * Runs in a daemon thread. Silently disabled if Kafka is unavailable.
 */
public class KafkaAuditLogConsumer implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(KafkaAuditLogConsumer.class.getName());
    private static final String GROUP_ID = "oms-audit-log-consumer";

    private final AuditLogRepository auditLogRepository;
    private final String bootstrapServers;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean running = true;
    private boolean enabled = false;

    public KafkaAuditLogConsumer(String bootstrapServers, AuditLogRepository auditLogRepository) {
        this.bootstrapServers = bootstrapServers;
        this.auditLogRepository = auditLogRepository;
    }

    public void start() {
        Thread thread = new Thread(this, "kafka-audit-log-consumer");
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
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(
                    KafkaOrderEventPublisher.TOPIC_ORDER_PLANNED,
                    KafkaOrderEventPublisher.TOPIC_ORDER_STATE,
                    KafkaOrderEventPublisher.TOPIC_BULK_CREATED,
                    KafkaOrderEventPublisher.TOPIC_BULK_TRANSMITTED,
                    KafkaOrderEventPublisher.TOPIC_ORDER_BOOKED
            ));
            enabled = true;
            LOGGER.info("KafkaAuditLogConsumer started, subscribed to all OMS topics");

            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    for (ConsumerRecord<String, String> record : records) {
                        handleRecord(record);
                    }
                } catch (Exception ex) {
                    if (running) {
                        LOGGER.warning("Kafka audit poll error: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("KafkaAuditLogConsumer failed to start: " + ex.getMessage());
            enabled = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRecord(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.getOrDefault("eventType", "UNKNOWN");
            String orderID = (String) event.getOrDefault("orderID",
                    event.getOrDefault("bulkOrderID", record.key()));
            String fromStatus = (String) event.get("fromStatus");
            String toStatus = (String) event.getOrDefault("orderStatus",
                    (String) event.get("status"));

            if (auditLogRepository != null && orderID != null) {
                AuditLogEntry entry = new AuditLogEntry(
                        orderID,
                        fromStatus,
                        toStatus != null ? toStatus : eventType,
                        "KafkaAuditLogConsumer",
                        "topic=" + record.topic() + ", eventType=" + eventType
                );
                auditLogRepository.log(entry);
                LOGGER.fine("Audit logged Kafka event: " + eventType + " for " + orderID);
            }
        } catch (Exception ex) {
            LOGGER.warning("Failed to audit-log Kafka record: " + ex.getMessage());
        }
    }
}
