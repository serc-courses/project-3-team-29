package com.iiit.oms.processor;

import com.iiit.oms.interfaces.SseBroadcaster;
import com.iiit.oms.kafka.KafkaOrderEventPublisher;
import com.iiit.oms.model.Fund;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderStatus;
import com.iiit.oms.readmodel.OrderProjectionListener;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.repository.OrderRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OrderScheduler {
    private static final Logger LOGGER = Logger.getLogger(OrderScheduler.class.getName());
    private static final long POLL_INTERVAL_SECONDS = 30;

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;

    // Optional dependencies for real-time SSE broadcasting
    private OrderProjectionListener projectionListener;
    private FundRepository fundRepository;
    private SseBroadcaster sseBroadcaster;
    private KafkaOrderEventPublisher kafkaPublisher;

    public OrderScheduler(OrderRepository orderRepository, OrderStateMachine orderStateMachine) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.orderStateMachine = Objects.requireNonNull(orderStateMachine, "orderStateMachine must not be null");
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void setProjectionListener(OrderProjectionListener projectionListener) {
        this.projectionListener = projectionListener;
    }

    public void setFundRepository(FundRepository fundRepository) {
        this.fundRepository = fundRepository;
    }

    public void setSseBroadcaster(SseBroadcaster sseBroadcaster) {
        this.sseBroadcaster = sseBroadcaster;
    }

    public void setKafkaPublisher(KafkaOrderEventPublisher kafkaPublisher) {
        this.kafkaPublisher = kafkaPublisher;
    }

    public int pollAndProcessPendingOrders() {
        LOGGER.info("OrderScheduler wake-up triggered. Scanning for pending orders.");

        List<Order> pendingOrders = orderRepository.findAll()
                .stream()
                .filter(order -> !order.isProcessed())
                .collect(Collectors.toList());

        LOGGER.info("OrderScheduler found " + pendingOrders.size() + " pending order(s)");

        for (Order order : pendingOrders) {
            try {
                OrderStatus beforeStatus = order.getOrderStatus();
                LOGGER.info("Polling order for processing: " + order);
                Order processedOrder = orderStateMachine.process(order);
                processedOrder.setProcessed(true);
                orderRepository.save(processedOrder);

                // Update projection + broadcast SSE events for real-time UI
                if (projectionListener != null && fundRepository != null) {
                    Optional<Fund> maybeFund = fundRepository.findByFundId(processedOrder.getProductID());
                    if (maybeFund.isPresent()) {
                        projectionListener.onOrderStatusChanged(processedOrder, null, maybeFund.get());
                        if (sseBroadcaster != null) {
                            sseBroadcaster.broadcastOrderUpdate(processedOrder, null);
                        }
                    }
                }
                // Publish Kafka event
                if (kafkaPublisher != null && kafkaPublisher.isEnabled()) {
                    kafkaPublisher.publishOrderStateChanged(processedOrder, beforeStatus.name());
                }
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, "Failed to process order: " + order.getOrderID(), ex);
            }
        }

        return pendingOrders.size();
    }

    public void startPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            LOGGER.warning("Polling already started");
            return;
        }

        LOGGER.info("Starting OrderScheduler with " + POLL_INTERVAL_SECONDS + " second interval");
        pollingTask = scheduler.scheduleAtFixedRate(
                this::pollAndProcessPendingOrders,
                0, // Initial delay
                POLL_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        LOGGER.info("OrderScheduler scheduled successfully");
    }

    public void stopPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            LOGGER.info("Stopping OrderScheduler");
            pollingTask.cancel(false);
        }
    }

    public void shutdown() {
        stopPolling();
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
}

