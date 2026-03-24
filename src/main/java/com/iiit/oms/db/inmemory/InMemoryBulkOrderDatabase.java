package com.iiit.oms.db.inmemory;

import com.iiit.oms.model.BulkOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBulkOrderDatabase {
    private final Map<String, BulkOrder> bulkOrders = new ConcurrentHashMap<>();

    public void upsert(BulkOrder bulkOrder) {
        bulkOrders.put(bulkOrder.getOrderID(), bulkOrder);
    }

    public Optional<BulkOrder> getById(String orderId) {
        return Optional.ofNullable(bulkOrders.get(orderId));
    }

    public List<BulkOrder> getAll() {
        return new ArrayList<>(bulkOrders.values());
    }

    public boolean exists(String orderId) {
        return bulkOrders.containsKey(orderId);
    }

    public void deleteById(String orderId) {
        bulkOrders.remove(orderId);
    }

    public void clear() {
        bulkOrders.clear();
    }
}
