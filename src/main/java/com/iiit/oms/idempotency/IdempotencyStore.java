package com.iiit.oms.idempotency;

/**
 * Provides idempotency checking for order submission.
 * Prevents the same logical order from being submitted multiple times
 * within the TTL window (e.g., double-click, retry storms).
 */
public interface IdempotencyStore {

    /**
     * Attempts to register a key. Returns true if this is a NEW key (first time seen).
     * Returns false if the key already exists (duplicate submission).
     *
     * @param key       the idempotency key (e.g. hash of accountID+productID+amount+side)
     * @param orderID   the order ID being associated with this key
     * @param ttlSeconds how long to keep the key before expiring it
     * @return true if key was newly registered, false if it was already present
     */
    boolean registerIfAbsent(String key, String orderID, int ttlSeconds);

    /**
     * Returns the order ID previously registered for this key, or null if not found / expired.
     */
    String getOrderId(String key);

    /**
     * Increment the dedup-hits counter (called when a duplicate is blocked).
     */
    void incrementDedupHits();

    /**
     * Returns the count of duplicate submissions that were blocked.
     */
    long getDedupHits();

    /**
     * Returns a short status string for health/debug endpoints.
     */
    String getStatus();
}

