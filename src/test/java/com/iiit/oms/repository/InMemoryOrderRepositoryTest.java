package com.iiit.oms.repository;

import com.iiit.oms.db.inmemory.InMemoryOrderDatabase;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.repository.inmemory.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryOrderRepositoryTest {

    @Test
    void shouldSaveAndRetrieveOrder() {
        OrderRepository repository = new InMemoryOrderRepository(new InMemoryOrderDatabase());

        Order order = new Order("ORD001", "FND001", BigDecimal.TEN, BigDecimal.valueOf(1000), "ACCT00001", OrderSide.BUY);
        Order savedOrder = repository.save(order);

        assertEquals("ORD001", savedOrder.getOrderID());
        assertEquals(OrderSide.BUY, savedOrder.getOrderSide());
        assertEquals(OrderStatus.PLANNED, savedOrder.getOrderStatus());
        assertEquals(false, savedOrder.isProcessed());
    }

    @Test
    void shouldRetrieveOrderByOrderId() {
        OrderRepository repository = new InMemoryOrderRepository(new InMemoryOrderDatabase());
        Order order = new Order("ORD002", "FND002", BigDecimal.ONE, BigDecimal.valueOf(500), "ACCT00002", OrderSide.SELL);
        repository.save(order);

        Optional<Order> retrieved = repository.findByOrderId("ORD002");

        assertTrue(retrieved.isPresent());
        assertEquals("ORD002", retrieved.get().getOrderID());
        assertEquals(OrderSide.SELL, retrieved.get().getOrderSide());
        assertEquals(false, retrieved.get().isProcessed());
    }

    @Test
    void shouldRetrieveAllOrders() {
        OrderRepository repository = new InMemoryOrderRepository(new InMemoryOrderDatabase());
        repository.save(new Order("ORD001", "FND001", BigDecimal.TEN, BigDecimal.valueOf(1000), "ACCT00001", OrderSide.BUY));
        repository.save(new Order("ORD002", "FND002", BigDecimal.ONE, BigDecimal.valueOf(500), "ACCT00002", OrderSide.SELL));

        List<Order> orders = repository.findAll();

        assertEquals(2, orders.size());
    }

    @Test
    void shouldCheckOrderExistence() {
        OrderRepository repository = new InMemoryOrderRepository(new InMemoryOrderDatabase());
        repository.save(new Order("ORD001", "FND001", BigDecimal.TEN, BigDecimal.valueOf(1000), "ACCT00001", OrderSide.BUY));

        assertTrue(repository.existsByOrderId("ORD001"));
    }

    @Test
    void shouldDeleteOrder() {
        OrderRepository repository = new InMemoryOrderRepository(new InMemoryOrderDatabase());
        repository.save(new Order("ORD001", "FND001", BigDecimal.TEN, BigDecimal.valueOf(1000), "ACCT00001", OrderSide.BUY));

        repository.deleteByOrderId("ORD001");

        assertEquals(0, repository.findAll().size());
    }
}
