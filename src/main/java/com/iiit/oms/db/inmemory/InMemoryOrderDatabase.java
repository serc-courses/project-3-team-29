package com.iiit.oms.db.inmemory;

import com.iiit.oms.model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderDatabase {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public void upsert(Order order) {
        //order.setProcessed(true);
        orders.put(order.getOrderID(), order);
    }

    public Optional<Order> getById(String orderID) {
        return Optional.ofNullable(orders.get(orderID));
    }

    public List<Order> getAll() {
        return new ArrayList<>(orders.values());
    }

    public boolean exists(String orderID) {
        return orders.containsKey(orderID);
    }

    public void deleteById(String orderID) {
        orders.remove(orderID);
    }

    public void clear() {
        orders.clear();
    }
}
