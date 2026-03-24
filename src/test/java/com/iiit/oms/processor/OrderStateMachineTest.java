package com.iiit.oms.processor;

import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.repository.AccountRepository;
import com.iiit.oms.repository.FundRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStateMachineTest {

    @Test
    void shouldTransitionOrderFromPlannedToPlaced() {
        Order order = new Order("ORD001", "FND001", BigDecimal.TEN, BigDecimal.valueOf(1000), "ACCT00001", OrderSide.BUY);
        
        // Create mock repositories that return true for existence checks
        AccountRepository mockAccountRepo = new MockAccountRepository(true);
        FundRepository mockFundRepo = new MockFundRepository(true);
        
        OrderManager manager = new OrderManager(mockAccountRepo, mockFundRepo);
        OrderStateMachine stateMachine = new OrderStateMachine(manager);

        stateMachine.process(order);

        assertEquals(OrderStatus.PLACED, order.getOrderStatus());
    }

    @Test
    void shouldMoveOrderToErroredWhenAnyStateFails() {
        Order order = new Order("ORD002", "FND002", BigDecimal.ONE, BigDecimal.valueOf(250), "ACCT00002", OrderSide.SELL);

        AccountRepository mockAccountRepo = new MockAccountRepository(true);
        FundRepository mockFundRepo = new MockFundRepository(true);
        
        OrderManager manager = new OrderManager(mockAccountRepo, mockFundRepo) {
            @Override
            public void place(Order order) {
                throw new IllegalStateException("Placement failed");
            }
        };

        OrderStateMachine stateMachine = new OrderStateMachine(manager);
        stateMachine.process(order);

        assertEquals(OrderStatus.ERRORED, order.getOrderStatus());
    }
    
    // Mock repository implementations for testing
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
        public java.util.List<com.iiit.oms.model.Account> findAll() {
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
        public java.util.List<com.iiit.oms.model.Fund> findAll() {
            return java.util.Collections.emptyList();
        }
        
        @Override
        public com.iiit.oms.model.Fund save(com.iiit.oms.model.Fund fund) {
            return fund;
        }
        
        @Override
        public Optional<com.iiit.oms.model.Fund> findByFundId(String fundID) {
            return Optional.empty();
        }
        
        @Override
        public void deleteByFundId(String fundID) {
        }
    }
}
