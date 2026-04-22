package com.iiit.oms.repository.postgres;

import com.iiit.oms.db.postgres.PostgresAuditLogDatabase;
import com.iiit.oms.model.AuditLogEntry;
import com.iiit.oms.repository.AuditLogRepository;

import java.util.List;
import java.util.Objects;

public class PostgresAuditLogRepository implements AuditLogRepository {
    private final PostgresAuditLogDatabase database;

    public PostgresAuditLogRepository(PostgresAuditLogDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public void log(AuditLogEntry entry) {
        database.insert(entry);
    }

    @Override
    public List<AuditLogEntry> findByOrderId(String orderID) {
        return database.findByOrderId(orderID);
    }

    /** Convenience method to also write transmission records. */
    public void logTransmission(String bulkOrderId, String transferAgent, String transmissionRef) {
        database.insertTransmissionLog(bulkOrderId, transferAgent, transmissionRef);
    }
}
