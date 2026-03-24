package com.iiit.oms.processor;

import com.iiit.oms.db.inmemory.InMemoryBulkOrderMappingDatabase;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.BulkOrderStatus;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.BulkOrderMappingRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.repository.inmemory.InMemoryBulkOrderMappingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchoutSchedulerTest {

    @Test
    void shouldCreateBulkOrdersGroupedByProductAndSideAndStoreMappings() {
        StubOrderRepository repository = new StubOrderRepository();
        repository.save(new Order("ORD001", "FND001", BigDecimal.valueOf(10), BigDecimal.valueOf(1000), "ACCT00001", OrderSide.BUY, OrderStatus.PLACED, true));
        repository.save(new Order("ORD002", "FND001", BigDecimal.valueOf(15), BigDecimal.valueOf(1800), "ACCT00002", OrderSide.BUY, OrderStatus.PLACED, true));
        repository.save(new Order("ORD003", "FND001", BigDecimal.valueOf(7), BigDecimal.valueOf(900), "ACCT00003", OrderSide.SELL, OrderStatus.PLACED, true));
        repository.save(new Order("ORD004", "FND002", BigDecimal.valueOf(5), BigDecimal.valueOf(500), "ACCT00004", OrderSide.BUY, OrderStatus.BOOKED, true));

        BulkOrderMappingRepository mappingRepository = new InMemoryBulkOrderMappingRepository(new InMemoryBulkOrderMappingDatabase());
        BulkOrderRepository bulkOrderRepository = new StubBulkOrderRepository();
        BatchoutScheduler scheduler = new BatchoutScheduler(repository, mappingRepository, bulkOrderRepository);

        List<BulkOrder> bulkOrders = scheduler.batchoutPlacedOrders();

        assertEquals(2, bulkOrders.size());

        BulkOrder buyBulkOrder = bulkOrders.stream()
                .filter(order -> "FND001".equals(order.getProductID()) && order.getOrderSide() == OrderSide.BUY)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing BUY bulk order for FND001"));

        assertEquals(BigDecimal.valueOf(25), buyBulkOrder.getQuantity());
        assertEquals(BigDecimal.valueOf(2800), buyBulkOrder.getAmount());
        assertEquals("FIRMACCT", buyBulkOrder.getAccountID());
        assertEquals(BulkOrderStatus.BULKED, buyBulkOrder.getBulkOrderStatus());

        Optional<List<String>> buyMapping = mappingRepository.findIndividualOrderIds(buyBulkOrder.getOrderID());
        assertTrue(buyMapping.isPresent());
        assertEquals(2, buyMapping.get().size());
        assertTrue(buyMapping.get().contains("ORD001"));
        assertTrue(buyMapping.get().contains("ORD002"));

        BulkOrder sellBulkOrder = bulkOrders.stream()
                .filter(order -> "FND001".equals(order.getProductID()) && order.getOrderSide() == OrderSide.SELL)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing SELL bulk order for FND001"));

        assertEquals(BigDecimal.valueOf(7), sellBulkOrder.getQuantity());
        assertEquals(BigDecimal.valueOf(900), sellBulkOrder.getAmount());
        assertEquals(BulkOrderStatus.BULKED, sellBulkOrder.getBulkOrderStatus());

        Optional<List<String>> sellMapping = mappingRepository.findIndividualOrderIds(sellBulkOrder.getOrderID());
        assertTrue(sellMapping.isPresent());
        assertEquals(1, sellMapping.get().size());
        assertTrue(sellMapping.get().contains("ORD003"));
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

    private static final class StubBulkOrderRepository implements BulkOrderRepository {
        @Override
        public BulkOrder save(BulkOrder bulkOrder) {
            return bulkOrder;
        }

        @Override
        public Optional<BulkOrder> findByOrderId(String orderID) {
            return Optional.empty();
        }

        @Override
        public List<BulkOrder> findAll() {
            return new ArrayList<>();
        }

        @Override
        public boolean existsByOrderId(String orderID) {
            return false;
        }

        @Override
        public void deleteByOrderId(String orderID) {
        }
    }
}
