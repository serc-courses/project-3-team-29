package com.iiit.oms.model;

import java.time.Instant;

/**
 * Represents a reconciliation break detected when comparing the expected
 * bulk order amount against the actual value (NAV × totalShares) received
 * from the Transfer Agent's contract callback.
 */
public class ReconciliationBreak {
    private String breakId;
    private String bulkOrderId;
    private String breakType; // AMOUNT_MISMATCH, FUND_MISMATCH
    private String expectedValue;
    private String receivedValue;
    private Instant detectedAt;
    private boolean resolved;
    private boolean escalated;

    public ReconciliationBreak() {
    }

    public ReconciliationBreak(String breakId, String bulkOrderId, String breakType,
            String expectedValue, String receivedValue, Instant detectedAt) {
        this.breakId = breakId;
        this.bulkOrderId = bulkOrderId;
        this.breakType = breakType;
        this.expectedValue = expectedValue;
        this.receivedValue = receivedValue;
        this.detectedAt = detectedAt;
        this.resolved = false;
        this.escalated = false;
    }

    public String getBreakId() {
        return breakId;
    }

    public void setBreakId(String breakId) {
        this.breakId = breakId;
    }

    public String getBulkOrderId() {
        return bulkOrderId;
    }

    public void setBulkOrderId(String bulkOrderId) {
        this.bulkOrderId = bulkOrderId;
    }

    public String getBreakType() {
        return breakType;
    }

    public void setBreakType(String breakType) {
        this.breakType = breakType;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getReceivedValue() {
        return receivedValue;
    }

    public void setReceivedValue(String receivedValue) {
        this.receivedValue = receivedValue;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    @Override
    public String toString() {
        return "ReconciliationBreak{" +
                "breakId='" + breakId + '\'' +
                ", bulkOrderId='" + bulkOrderId + '\'' +
                ", breakType='" + breakType + '\'' +
                ", expectedValue='" + expectedValue + '\'' +
                ", receivedValue='" + receivedValue + '\'' +
                ", detectedAt=" + detectedAt +
                ", resolved=" + resolved +
                ", escalated=" + escalated +
                '}';
    }
}
