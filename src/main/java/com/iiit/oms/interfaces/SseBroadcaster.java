package com.iiit.oms.interfaces;

import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.Order;
import java.util.List;

/**
 * Abstraction for broadcasting SSE events to connected clients.
 * Allows OrderScheduler and BatchoutScheduler to push real-time updates
 * without directly depending on the HTTP server.
 */
public interface SseBroadcaster {
    void broadcastOrderUpdate(Order order, String bulkOrderID);
    void broadcastBulkOrderUpdate(BulkOrder bulkOrder, List<String> mappedOrderIDs);
    /** Broadcast a raw SSE event with a pre-built JSON payload string. */
    void broadcastRawEvent(String eventType, String jsonPayload);
}
