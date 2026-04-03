package com.iiit.oms.readmodel.impl;

import com.iiit.oms.readmodel.*;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.Fund;
import java.util.List;

/**
 * Default implementation of OrderProjectionListener.
 * Synchronizes write-side Order events to read-side views via ProjectionStore.
 * Handles all order lifecycle events:
 * - Order creation/planning
 * - Order status changes (PLANNED → CONFIRMED → BOOKED)
 * - Bulk order creation and status changes
 * - Deletion event
 */
public class DefaultOrderProjectionListener implements OrderProjectionListener {

    private final ProjectionStore projectionStore;

    public DefaultOrderProjectionListener(ProjectionStore projectionStore) {
        this.projectionStore = projectionStore;
    }

    @Override
    public void onOrderPlanned(Order order, Fund fund) {
        // First time we see this order - project it to read model
        projectionStore.projectOrder(order, null, fund);
    }

    @Override
    public void onOrderStatusChanged(Order order, BulkOrder bulkOrder, Fund fund) {
        // Order has transitioned state - update its view
        projectionStore.updateOrderView(order.getOrderID(), order, fund, bulkOrder);
    }

    @Override
    public void onBulkOrderCreated(BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs) {
        // New bulk order formed - project it to read model
        projectionStore.projectBulkOrder(bulkOrder, fund, matchedOrderIDs);
    }

    @Override
    public void onBulkOrderStatusChanged(BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs) {
        // Bulk order has transitioned state - update its view
        projectionStore.updateBulkOrderView(bulkOrder.getOrderID(), bulkOrder, fund, matchedOrderIDs);
    }

    @Override
    public void onOrderDeleted(String orderID) {
        // Order is no longer relevant - remove from read model
        projectionStore.deleteOrderView(orderID);
    }

    @Override
    public void onBulkOrderDeleted(String bulkOrderID) {
        // Bulk order is no longer relevant - remove from read model
        projectionStore.deleteBulkOrderView(bulkOrderID);
    }
}
