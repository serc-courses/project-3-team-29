package com.iiit.oms.model;

import java.time.Instant;

/**
 * Immutable audit log entry recording a single state transition for an order.
 * Written on every OrderStatus change; never updated or deleted.
 */
public class AuditLogEntry {
    private final String orderID;
    private final String fromStatus;
    private final String toStatus;
    private final Instant occurredAt;
    private final String actor;       // system component that triggered the transition
    private final String details;     // optional context (e.g. contract ref, error message)

    public AuditLogEntry(String orderID, String fromStatus, String toStatus,
                         String actor, String details) {
        this.orderID = orderID;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.occurredAt = Instant.now();
        this.actor = actor;
        this.details = details;
    }

    public String getOrderID() { return orderID; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActor() { return actor; }
    public String getDetails() { return details; }

    @Override
    public String toString() {
        return "AuditLogEntry{orderID=" + orderID + ", " + fromStatus + "→" + toStatus
                + ", actor=" + actor + ", at=" + occurredAt + "}";
    }
}
