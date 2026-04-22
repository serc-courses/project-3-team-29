error id: file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/BatchoutScheduler.java:_empty_/BulkOrderStatus#TRANSMITTED#
file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/BatchoutScheduler.java
empty definition using pc, found symbol in pc: _empty_/BulkOrderStatus#TRANSMITTED#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 7914
uri: file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/BatchoutScheduler.java
text:
```scala
package com.iiit.oms.processor;

import com.iiit.oms.model.AuditLogEntry;
import com.iiit.oms.model.BulkOrder;
import com.iiit.oms.model.BulkOrderStatus;
import com.iiit.oms.model.Fund;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.readmodel.OrderProjectionListener;
import com.iiit.oms.repository.AuditLogRepository;
import com.iiit.oms.repository.BulkOrderRepository;
import com.iiit.oms.repository.BulkOrderMappingRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.transfer.TransferAgentRouter;
import com.iiit.oms.transfer.TransmissionAck;
import com.iiit.oms.util.UniqueIdGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final FundRepository fundRepository;
    private final OrderProjectionListener projectionListener;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> batchoutTask;

    // Optional dependencies wired after construction
    private TransferAgentRouter transferAgentRouter;
    private AuditLogRepository auditLogRepository;

    public BatchoutScheduler(OrderRepository orderRepository, BulkOrderMappingRepository bulkOrderMappingRepository, BulkOrderRepository bulkOrderRepository) {
        this(orderRepository, bulkOrderMappingRepository, bulkOrderRepository, null, null);
    }

    public BatchoutScheduler(OrderRepository orderRepository,
                             BulkOrderMappingRepository bulkOrderMappingRepository,
                             BulkOrderRepository bulkOrderRepository,
                             FundRepository fundRepository,
                             OrderProjectionListener projectionListener) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.bulkOrderMappingRepository = Objects.requireNonNull(bulkOrderMappingRepository, "bulkOrderMappingRepository must not be null");
        this.bulkOrderRepository = Objects.requireNonNull(bulkOrderRepository, "bulkOrderRepository must not be null");
        this.fundRepository = fundRepository;
        this.projectionListener = projectionListener;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void setTransferAgentRouter(TransferAgentRouter transferAgentRouter) {
        this.transferAgentRouter = transferAgentRouter;
    }

    public void setAuditLogRepository(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
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

            // Determine transfer agent from the first order in the batch (all same fund → same TA)
            String transferAgent = ordersForBatch.stream()
                    .map(Order::getTransferAgent)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("NSCC");

            BulkOrder bulkOrder = new BulkOrder(
                    bulkOrderId,
                    key.productID,
                    key.orderSide,
                    BulkOrderStatus.BULKED,
                    aggregatedQuantity,
                    aggregatedAmount,
                    FIRM_ACCOUNT_ID
            );
            bulkOrder.setTransferAgent(transferAgent);

            List<String> constituentOrderIds = ordersForBatch.stream()
                    .map(Order::getOrderID)
                    .collect(Collectors.toList());

            bulkOrderRepository.save(bulkOrder);
            bulkOrderMappingRepository.save(bulkOrderId, constituentOrderIds);
            Optional<Fund> maybeFund = findFund(key.productID);

            // Mark constituent orders as BULKED
            for (Order individualOrder : ordersForBatch) {
                OrderStatus prevStatus = individualOrder.getOrderStatus();
                individualOrder.setOrderStatus(OrderStatus.BULKED);
                orderRepository.save(individualOrder);
                writeOrderAuditLog(individualOrder.getOrderID(), prevStatus, OrderStatus.BULKED, "BatchoutScheduler", "bulkOrderId=" + bulkOrderId);
                if (projectionListener != null && maybeFund.isPresent()) {
                    projectionListener.onOrderStatusChanged(individualOrder, bulkOrder, maybeFund.get());
                }
            }

            if (projectionListener != null && maybeFund.isPresent()) {
                projectionListener.onBulkOrderCreated(bulkOrder, maybeFund.get(), constituentOrderIds);
            }

            // ---- AUTO-TRANSMIT to Transfer Agent ----
            transmitBulkOrder(bulkOrder, ordersForBatch, maybeFund.orElse(null), constituentOrderIds);

            createdBulkOrders.add(bulkOrder);
            LOGGER.info("Created bulk order " + bulkOrderId + " for product " + key.productID
                    + " side=" + key.orderSide + " ta=" + transferAgent + " constituents=" + constituentOrderIds.size());
        }

        return createdBulkOrders;
    }

    private void transmitBulkOrder(BulkOrder bulkOrder, List<Order> ordersForBatch,
                                   Fund fund, List<String> constituentOrderIds) {
        if (transferAgentRouter == null) {
            LOGGER.warning("TransferAgentRouter not configured – skipping auto-transmit for " + bulkOrder.getOrderID());
            return;
        }
        try {
            TransmissionAck ack = transferAgentRouter.transmit(bulkOrder);
            if (ack.isAccepted()) {
                bulkOrder.setTransmissionRef(ack.getReferenceNumber());
                bulkOrder.setBulkOrderStatus(BulkOrderStatus.@@TRANSMITTED);
                bulkOrderRepository.save(bulkOrder);

                // Log transmission
                if (auditLogRepository instanceof com.iiit.oms.repository.postgres.PostgresAuditLogRepository) {
                    ((com.iiit.oms.repository.postgres.PostgresAuditLogRepository) auditLogRepository)
                            .logTransmission(bulkOrder.getOrderID(), ack.getTransferAgent(), ack.getReferenceNumber());
                }

                // Mark individual orders as TRANSMITTED
                for (Order order : ordersForBatch) {
                    OrderStatus prevStatus = order.getOrderStatus();
                    order.setOrderStatus(OrderStatus.TRANSMITTED);
                    orderRepository.save(order);
                    writeOrderAuditLog(order.getOrderID(), prevStatus, OrderStatus.TRANSMITTED, "BatchoutScheduler",
                            "transmissionRef=" + ack.getReferenceNumber() + ", ta=" + ack.getTransferAgent());
                }

                // Notify projection store
                if (projectionListener != null && fund != null) {
                    projectionListener.onBulkOrderCreated(bulkOrder, fund, constituentOrderIds);
                    for (Order order : ordersForBatch) {
                        projectionListener.onOrderStatusChanged(order, bulkOrder, fund);
                    }
                }
                LOGGER.info("Bulk order " + bulkOrder.getOrderID() + " transmitted to " + ack.getTransferAgent()
                        + " ref=" + ack.getReferenceNumber());
            } else {
                LOGGER.severe("TA rejected bulk order " + bulkOrder.getOrderID() + ": " + ack.getMessage());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to transmit bulk order " + bulkOrder.getOrderID(), ex);
        }
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

    private void runBatchoutCycle() {
        try {
            LOGGER.info("BatchoutScheduler wake-up triggered");
            List<BulkOrder> created = batchoutPlacedOrders();
            LOGGER.info("BatchoutScheduler created " + created.size() + " bulk order(s)");
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "BatchoutScheduler failed to create bulk orders", ex);
        }
    }

    private Optional<Fund> findFund(String productID) {
        if (fundRepository == null) return Optional.empty();
        return fundRepository.findByFundId(productID);
    }

    private void writeOrderAuditLog(String orderID, OrderStatus from, OrderStatus to, String actor, String details) {
        if (auditLogRepository == null) return;
        try {
            auditLogRepository.log(new AuditLogEntry(orderID, from != null ? from.name() : null, to.name(), actor, details));
        } catch (Exception ex) {
            LOGGER.warning("Failed to write audit log for order " + orderID + ": " + ex.getMessage());
        }
    }

    private static final class BatchKey {
        final String productID;
        final OrderSide orderSide;

        BatchKey(String productID, OrderSide orderSide) {
            this.productID = productID;
            this.orderSide = orderSide;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BatchKey)) return false;
            BatchKey that = (BatchKey) o;
            return Objects.equals(productID, that.productID) && orderSide == that.orderSide;
        }

        @Override
        public int hashCode() {
            return Objects.hash(productID, orderSide);
        }
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/BulkOrderStatus#TRANSMITTED#