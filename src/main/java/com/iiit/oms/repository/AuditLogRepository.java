package com.iiit.oms.repository;

import com.iiit.oms.model.AuditLogEntry;

import java.util.List;

/**
 * Repository for the immutable order audit trail.
 */
public interface AuditLogRepository {
    void log(AuditLogEntry entry);
    List<AuditLogEntry> findByOrderId(String orderID);
}
