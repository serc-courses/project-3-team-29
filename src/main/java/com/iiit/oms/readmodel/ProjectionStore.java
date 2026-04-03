package com.iiit.oms.readmodel;

import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.Fund;
import java.util.List;
import java.util.Optional;

/**
 * Projection store interface for CQRS read-side.
 * Bridge between write-side (Order processing) and read-side (View models).
 * Implementations can use in-memory, MongoDB, or any persistence layer.
 */
public interface ProjectionStore {

    // OrderView operations
    void projectOrder(Order order, BulkOrder bulkOrder, Fund fund);
    void updateOrderView(String orderID, Order order, Fund fund, BulkOrder bulkOrder);
    Optional<OrderView> findOrderView(String orderID);
    List<OrderView> findOrdersByAccount(String accountID);
    List<OrderView> findOrdersByFund(String fundID);
    List<OrderView> findOrdersByBulkOrder(String bulkOrderID);
    List<OrderView> findAllOrderViews();
    void deleteOrderView(String orderID);

    // BulkOrderView operations
    void projectBulkOrder(BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs);
    void updateBulkOrderView(String bulkOrderID, BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs);
    Optional<BulkOrderView> findBulkOrderView(String bulkOrderID);
    List<BulkOrderView> findBulkOrdersByFund(String fundID);
    List<BulkOrderView> findAllBulkOrderViews();
    void deleteBulkOrderView(String bulkOrderID);

    // Maintenance
    void clearAll();
    String getStatus();
}
