package com.iiit.oms.repository;

import com.iiit.oms.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    // TODO: Add repository support to retrieve Orders based on the isProcessed field.

    Optional<Order> findByOrderId(String orderID);

    List<Order> findAll();

    boolean existsByOrderId(String orderID);

    void deleteByOrderId(String orderID);
}
