package com.iiit.oms.processor;

import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.repository.AccountRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderSchedulerTest {

    @Test
    void shouldPollOnlyUnprocessedOrdersAndProcessThem() {
        StubOrderRepository repository = new StubOrderRepository();
        repository.save(new Order("ORD001", "FND001", BigDecimal.ONE, BigDecimal.TEN, "ACCT00001", OrderSide.BUY,
                OrderStatus.PLANNED, false));
        repository.save(new Order("ORD002", "FND002", BigDecimal.ONE, BigDecimal.TEN, "ACCT00002", OrderSide.SELL,
                OrderStatus.PLANNED, true));

        TrackingOrderStateMachine stateMachine = new TrackingOrderStateMachine();
        OrderScheduler scheduler = new OrderScheduler(repository, stateMachine);

        int processedCount = scheduler.pollAndProcessPendingOrders();

        assertEquals(1, processedCount);
        assertEquals(1, stateMachine.getProcessedOrderIds().size());
        assertEquals("ORD001", stateMachine.getProcessedOrderIds().get(0));

        Optional<Order> order1 = repository.findByOrderId("ORD001");
        Optional<Order> order2 = repository.findByOrderId("ORD002");
        assertTrue(order1.isPresent());
        assertTrue(order1.get().isProcessed());
        assertTrue(order2.isPresent());
        assertTrue(order2.get().isProcessed());
    }

    private static final class TrackingOrderStateMachine extends OrderStateMachine {
        private final List<String> processedOrderIds = new ArrayList<>();

        private TrackingOrderStateMachine() {
            super(new OrderManager(new MockAccountRepository(true), new MockFundRepository(true), null));
        }

        @Override
        public Order process(Order order) {
            processedOrderIds.add(order.getOrderID());
            order.setOrderStatus(OrderStatus.BOOKED);
            return order;
        }

        private List<String> getProcessedOrderIds() {
            return processedOrderIds;
        }
    }

    private static class MockAccountRepository implements AccountRepository {
        private final boolean exists;

        public MockAccountRepository(boolean exists) {
            this.exists = exists;
        }

        @Override
        public boolean existsByAccountId(String accountID) {
            return exists;
        }

        @Override
        public List<com.iiit.oms.model.Account> findAll() {
            return java.util.Collections.emptyList();
        }

        @Override
        public com.iiit.oms.model.Account save(com.iiit.oms.model.Account account) {
            return account;
        }

        @Override
        public Optional<com.iiit.oms.model.Account> findByAccountId(String accountID) {
            return Optional.empty();
        }

        @Override
        public void deleteByAccountId(String accountID) {
        }
    }

    private static class MockFundRepository implements FundRepository {
        private final boolean exists;

        public MockFundRepository(boolean exists) {
            this.exists = exists;
        }

        @Override
        public boolean existsByFundId(String fundID) {
            return exists;
        }

        @Override
        public List<com.iiit.oms.model.Fund> findAll() {
            return java.util.Collections.emptyList();
        }

        @Override
        public void deleteByFundId(String fundID) {
        }

        @Override
        public com.iiit.oms.model.Fund save(com.iiit.oms.model.Fund fund) {
            return fund;
        }

        @Override
        public Optional<com.iiit.oms.model.Fund> findByFundId(String fundID) {
            return Optional.empty();
        }
    }

    private static final class StubOrderRepository implements OrderRepository {
        private final List<Order> orders = new ArrayList<>();

        @Override
        public Order save(Order order) {
            deleteByOrderId(order.getOrderID());
            orders.add(order);
            return order;
        }

        @Override
        public Optional<Order> findByOrderId(String orderID) {
            return orders.stream().filter(order -> orderID.equals(order.getOrderID())).findFirst();
        }

        @Override
        public List<Order> findAll() {
            return new ArrayList<>(orders);
        }

        @Override
        public boolean existsByOrderId(String orderID) {
            return orders.stream().anyMatch(order -> orderID.equals(order.getOrderID()));
        }

        @Override
        public void deleteByOrderId(String orderID) {
            orders.removeIf(order -> orderID.equals(order.getOrderID()));
        }
    }
}
