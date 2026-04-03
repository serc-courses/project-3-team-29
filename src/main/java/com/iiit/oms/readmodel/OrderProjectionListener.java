package com.iiit.oms.readmodel;

import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.Fund;
import java.util.List;

/**
 * Listener interface for order projection events.
 * Implementations react to write-side Order/BulkOrder events and update read models.
 * Part of the CQRS pattern - bridges write-side to read-side.
 */
public interface OrderProjectionListener {

    /**
     * Called when an order is first created/planned
     */
    void onOrderPlanned(Order order, Fund fund);

    /**
     * Called when an order's status changes
     */
    void onOrderStatusChanged(Order order, BulkOrder bulkOrder, Fund fund);

    /**
     * Called when a bulk order is created
     */
    void onBulkOrderCreated(BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs);

    /**
     * Called when a bulk order's status changes
     */
    void onBulkOrderStatusChanged(BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs);

    /**
     * Called when an order is deleted
     */
    void onOrderDeleted(String orderID);

    /**
     * Called when a bulk order is deleted
     */
    void onBulkOrderDeleted(String bulkOrderID);
}
