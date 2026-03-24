package com.iiit.oms.repository.postgres;

import com.iiit.oms.db.postgres.PostgresBulkOrderDatabase;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.repository.BulkOrderRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PostgresBulkOrderRepository implements BulkOrderRepository {
    private final PostgresBulkOrderDatabase database;

    public PostgresBulkOrderRepository(PostgresBulkOrderDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public BulkOrder save(BulkOrder bulkOrder) {
        Objects.requireNonNull(bulkOrder, "bulkOrder must not be null");
        Objects.requireNonNull(bulkOrder.getOrderID(), "orderID must not be null");
        database.upsert(bulkOrder);
        return bulkOrder;
    }

    @Override
    public Optional<BulkOrder> findByOrderId(String orderID) {
        return database.getById(orderID);
    }

    @Override
    public List<BulkOrder> findAll() {
        return database.getAll();
    }

    @Override
    public boolean existsByOrderId(String orderID) {
        return database.exists(orderID);
    }

    @Override
    public void deleteByOrderId(String orderID) {
        database.deleteById(orderID);
    }
}
