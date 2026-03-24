package com.iiit.oms.repository.postgres;

import com.iiit.oms.db.postgres.PostgresOrderDatabase;
import com.iiit.oms.model.Order;
import com.iiit.oms.repository.OrderRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PostgresOrderRepository implements OrderRepository {
    private final PostgresOrderDatabase database;

    public PostgresOrderRepository(PostgresOrderDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public Order save(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(order.getOrderID(), "orderID must not be null");
        database.upsert(order);
        return order;
    }

    @Override
    public Optional<Order> findByOrderId(String orderID) {
        return database.getById(orderID);
    }

    @Override
    public List<Order> findAll() {
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
