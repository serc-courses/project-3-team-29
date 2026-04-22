package com.iiit.oms.transfer;

import java.time.Instant;

/**
 * Acknowledgement returned by a Transfer Agent after accepting a bulk order transmission.
 */
public class TransmissionAck {
    private final String referenceNumber;
    private final String status;        // "ACCEPTED", "REJECTED"
    private final String transferAgent; // "NSCC" or "RBC"
    private final Instant timestamp;
    private final String message;

    public TransmissionAck(String referenceNumber, String status, String transferAgent, String message) {
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.transferAgent = transferAgent;
        this.timestamp = Instant.now();
        this.message = message;
    }

    public String getReferenceNumber() { return referenceNumber; }
    public String getStatus() { return status; }
    public String getTransferAgent() { return transferAgent; }
    public Instant getTimestamp() { return timestamp; }
    public String getMessage() { return message; }

    public boolean isAccepted() { return "ACCEPTED".equals(status); }

    @Override
    public String toString() {
        return "TransmissionAck{ref=" + referenceNumber + ", status=" + status + ", ta=" + transferAgent + "}";
    }
}
