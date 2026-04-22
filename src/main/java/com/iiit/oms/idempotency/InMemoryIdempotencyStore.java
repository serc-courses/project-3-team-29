package com.iiit.oms.idempotency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * In-memory idempotency store. Used as a fallback when Redis is unavailable.
 * NOTE: This does NOT survive JVM restarts and is NOT shared across multiple instances.
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {
    private static final Logger LOGGER = Logger.getLogger(InMemoryIdempotencyStore.class.getName());

    private static final class Entry {
        final String orderID;
        final long expiresAt; // epoch millis

        Entry(String orderID, long expiresAt) {
            this.orderID = orderID;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final AtomicLong dedupHits = new AtomicLong(0);

    @Override
    public boolean registerIfAbsent(String key, String orderID, int ttlSeconds) {
        // Purge expired entries occasionally (simple approach: check on every write)
        store.entrySet().removeIf(e -> e.getValue().isExpired());

        Entry newEntry = new Entry(orderID, System.currentTimeMillis() + (long) ttlSeconds * 1000);
        Entry existing = store.putIfAbsent(key, newEntry);
        if (existing != null && !existing.isExpired()) {
            LOGGER.fine("Idempotency key already exists: " + key + " → orderID=" + existing.orderID);
            return false; // duplicate
        }
        if (existing != null && existing.isExpired()) {
            // Replace expired entry
            store.put(key, newEntry);
        }
        return true; // newly registered
    }

    @Override
    public String getOrderId(String key) {
        Entry entry = store.get(key);
        if (entry == null || entry.isExpired()) return null;
        return entry.orderID;
    }

    @Override
    public void incrementDedupHits() {
        dedupHits.incrementAndGet();
    }

    @Override
    public long getDedupHits() {
        return dedupHits.get();
    }

    @Override
    public String getStatus() {
        return "IN_MEMORY (keys=" + store.size() + ", dedupHits=" + dedupHits.get() + ")";
    }
}

