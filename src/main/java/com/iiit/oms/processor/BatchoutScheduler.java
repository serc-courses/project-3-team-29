package com.iiit.oms.processor;

import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.BulkOrderStatus;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.BulkOrderMappingRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.util.UniqueIdGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
//import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BatchoutScheduler {
    private static final Logger LOGGER = Logger.getLogger(BatchoutScheduler.class.getName());
    private static final long BATCHOUT_INTERVAL_SECONDS = 120;
    private static final String FIRM_ACCOUNT_ID = "FIRMACCT";

    private final OrderRepository orderRepository;
    private final BulkOrderMappingRepository bulkOrderMappingRepository;
    private final BulkOrderRepository bulkOrderRepository;
    private final ScheduledExecutorService scheduler;
    //private final Map<String, BulkOrder> bulkOrders;
    private ScheduledFuture<?> batchoutTask;

    public BatchoutScheduler(OrderRepository orderRepository, BulkOrderMappingRepository bulkOrderMappingRepository, BulkOrderRepository bulkOrderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.bulkOrderMappingRepository = Objects.requireNonNull(bulkOrderMappingRepository, "bulkOrderMappingRepository must not be null");
        this.bulkOrderRepository = Objects.requireNonNull(bulkOrderRepository, "bulkOrderRepository must not be null");
        this.scheduler = Executors.newScheduledThreadPool(1);
        //this.bulkOrders = new ConcurrentHashMap<>();
    }

    public List<BulkOrder> batchoutPlacedOrders() {
        List<Order> placedOrders = orderRepository.findAll()
                .stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.PLACED)
                .collect(Collectors.toList());

        LOGGER.info("BatchoutScheduler found " + placedOrders.size() + " PLACED order(s)");

        Map<BatchKey, List<Order>> groupedOrders = placedOrders.stream()
                .collect(Collectors.groupingBy(order -> new BatchKey(order.getProductID(), order.getOrderSide())));

        List<BulkOrder> createdBulkOrders = new ArrayList<>();

        for (Map.Entry<BatchKey, List<Order>> entry : groupedOrders.entrySet()) {
            BatchKey key = entry.getKey();
            List<Order> ordersForBatch = entry.getValue();

            BigDecimal aggregatedQuantity = ordersForBatch.stream()
                    .map(Order::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal aggregatedAmount = ordersForBatch.stream()
                    .map(Order::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String bulkOrderId = UniqueIdGenerator.generate("BLK");
            BulkOrder bulkOrder = new BulkOrder(
                    bulkOrderId,
                    key.productID,
                    key.orderSide,
                    BulkOrderStatus.BULKED,
                    aggregatedQuantity,
                    aggregatedAmount,
                    FIRM_ACCOUNT_ID
            );

            List<String> constituentOrderIds = ordersForBatch.stream()
                    .map(Order::getOrderID)
                    .collect(Collectors.toList());

            //bulkOrders.put(bulkOrderId, bulkOrder);
            bulkOrderRepository.save(bulkOrder);
            bulkOrderMappingRepository.save(bulkOrderId, constituentOrderIds);

            // Mark constituent orders as BULKED once they are grouped into a bulk order.
            for (Order individualOrder : ordersForBatch) {
                individualOrder.setOrderStatus(OrderStatus.BULKED);
                orderRepository.save(individualOrder);
            }

            createdBulkOrders.add(bulkOrder);

            LOGGER.info("Created bulk order " + bulkOrderId + " for product " + key.productID
                    + " and side " + key.orderSide + " with " + constituentOrderIds.size() + " order(s)");
        }

        return createdBulkOrders;
    }

    public void startBatchout() {
        if (batchoutTask != null && !batchoutTask.isCancelled()) {
            LOGGER.warning("BatchoutScheduler already started");
            return;
        }

        LOGGER.info("Starting BatchoutScheduler with " + BATCHOUT_INTERVAL_SECONDS + " second interval");
        batchoutTask = scheduler.scheduleAtFixedRate(
                this::runBatchoutCycle,
                0,
                BATCHOUT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public void stopBatchout() {
        if (batchoutTask != null && !batchoutTask.isCancelled()) {
            LOGGER.info("Stopping BatchoutScheduler");
            batchoutTask.cancel(false);
        }
    }

    public void shutdown() {
        stopBatchout();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /*public List<BulkOrder> findAllBulkOrders1() {
        return new ArrayList<>(bulkOrders.values());
    }*/

    private void runBatchoutCycle() {
        try {
            LOGGER.info("BatchoutScheduler wake-up triggered");
            List<BulkOrder> created = batchoutPlacedOrders();
            LOGGER.info("BatchoutScheduler created " + created.size() + " bulk order(s)");
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "BatchoutScheduler failed to create bulk orders", ex);
        }
    }

    private static final class BatchKey {
        private final String productID;
        private final OrderSide orderSide;

        private BatchKey(String productID, OrderSide orderSide) {
            this.productID = productID;
            this.orderSide = orderSide;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BatchKey)) {
                return false;
            }
            BatchKey batchKey = (BatchKey) o;
            return Objects.equals(productID, batchKey.productID) && orderSide == batchKey.orderSide;
        }

        @Override
        public int hashCode() {
            return Objects.hash(productID, orderSide);
        }
    }
}
