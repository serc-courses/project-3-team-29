package com.iiit.oms.readmodel.impl;

import com.iiit.oms.readmodel.*;
import com.iiit.oms.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ProjectionStore for CQRS read-side.
 * Suitable for POC and lightweight deployments.
 * Can be swapped for MongoDB-based implementation for production.
 */
public class InMemoryProjectionStore implements ProjectionStore {

    private final ConcurrentHashMap<String, OrderView> orderViews = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BulkOrderView> bulkOrderViews = new ConcurrentHashMap<>();

    @Override
    public void projectOrder(Order order, BulkOrder bulkOrder, Fund fund) {
        OrderView view = new OrderView(
                order.getOrderID(),
                order.getAccountID(),
                fund.getFundID(),
                fund.getFundName(),
                order.getOrderSide().toString(),
                order.getAmount(),
                order.getQuantity(),
                fund.getNAV(),
                order.getOrderStatus().toString(),
                bulkOrder != null ? bulkOrder.getOrderID() : null
        );
        orderViews.put(order.getOrderID(), view);
    }

    @Override
    public void updateOrderView(String orderID, Order order, Fund fund, BulkOrder bulkOrder) {
        OrderView view = orderViews.get(orderID);
        if (view != null) {
            view.setOrderStatus(order.getOrderStatus().toString());
            view.setQuantity(order.getQuantity());
            view.setAmount(order.getAmount());
            view.setNAV(fund.getNAV());
            view.setBulkOrderID(bulkOrder != null ? bulkOrder.getOrderID() : null);
        }
    }

    @Override
    public Optional<OrderView> findOrderView(String orderID) {
        return Optional.ofNullable(orderViews.get(orderID));
    }

    @Override
    public List<OrderView> findOrdersByAccount(String accountID) {
        return orderViews.values().stream()
                .filter(v -> v.getAccountID().equals(accountID))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderView> findOrdersByFund(String fundID) {
        return orderViews.values().stream()
                .filter(v -> v.getFundID().equals(fundID))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderView> findOrdersByBulkOrder(String bulkOrderID) {
        return orderViews.values().stream()
                .filter(v -> bulkOrderID.equals(v.getBulkOrderID()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderView> findAllOrderViews() {
        return new ArrayList<>(orderViews.values());
    }

    @Override
    public void deleteOrderView(String orderID) {
        orderViews.remove(orderID);
    }

    @Override
    public void projectBulkOrder(BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs) {
        BulkOrderView view = new BulkOrderView(
                bulkOrder.getOrderID(),
                fund.getFundID(),
                fund.getFundName(),
                bulkOrder.getOrderSide().toString(),
                bulkOrder.getBulkOrderStatus().toString(),
                bulkOrder.getAmount(),
                bulkOrder.getQuantity(),
                fund.getNAV(),
                matchedOrderIDs,
                matchedOrderIDs != null ? matchedOrderIDs.size() : 0
        );
        bulkOrderViews.put(bulkOrder.getOrderID(), view);
    }

    @Override
    public void updateBulkOrderView(String bulkOrderID, BulkOrder bulkOrder, Fund fund, List<String> matchedOrderIDs) {
        BulkOrderView view = bulkOrderViews.get(bulkOrderID);
        if (view != null) {
            view.setBulkOrderStatus(bulkOrder.getBulkOrderStatus().toString());
            view.setTotalAmount(bulkOrder.getAmount());
            view.setTotalQuantity(bulkOrder.getQuantity());
            view.setNAV(fund.getNAV());
            view.setMatchedOrderIDs(matchedOrderIDs);
            view.setMatchedOrderCount(matchedOrderIDs != null ? matchedOrderIDs.size() : 0);
        }
    }

    @Override
    public Optional<BulkOrderView> findBulkOrderView(String bulkOrderID) {
        return Optional.ofNullable(bulkOrderViews.get(bulkOrderID));
    }

    @Override
    public List<BulkOrderView> findBulkOrdersByFund(String fundID) {
        return bulkOrderViews.values().stream()
                .filter(v -> v.getFundID().equals(fundID))
                .collect(Collectors.toList());
    }

    @Override
    public List<BulkOrderView> findAllBulkOrderViews() {
        return new ArrayList<>(bulkOrderViews.values());
    }

    @Override
    public void deleteBulkOrderView(String bulkOrderID) {
        bulkOrderViews.remove(bulkOrderID);
    }

    @Override
    public void clearAll() {
        orderViews.clear();
        bulkOrderViews.clear();
    }

    @Override
    public String getStatus() {
        return String.format("InMemoryProjectionStore{orders=%d, bulkOrders=%d}",
                orderViews.size(), bulkOrderViews.size());
    }
}
