package com.iiit.oms.processor;

import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderStatus;

import java.util.Objects;
import java.util.logging.Logger;

public class OrderStateMachine {
    private static final Logger LOGGER = Logger.getLogger(OrderStateMachine.class.getName());
    private final OrderManager orderManager;

    public OrderStateMachine(OrderManager orderManager) {
        this.orderManager = Objects.requireNonNull(orderManager, "orderManager must not be null");
    }

    public Order process(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        LOGGER.info("Starting processing for order: " + order.getOrderID() + " with status: " + order.getOrderStatus());

        try {
            while (isOmsInternal(order.getOrderStatus())) {
                advance(order);
            }
            LOGGER.info("Order " + order.getOrderID() + " processing completed with final status: " + order.getOrderStatus());
        } catch (RuntimeException ex) {
            LOGGER.severe("Error processing order " + order.getOrderID() + ": " + ex.getMessage());
            order.setOrderStatus(OrderStatus.ERRORED);
            order.setErrorDescription(ex.getMessage());
        }

        return order;
    }

    public Order processBooking(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        LOGGER.info("Starting booking processing for order: " + order.getOrderID() + " with status: " + order.getOrderStatus());

        try {
            if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
                throw new IllegalStateException("Order must be CONFIRMED for booking, found: " + order.getOrderStatus());
            }
            advance(order);
        } catch (RuntimeException ex) {
            LOGGER.severe("Error booking order " + order.getOrderID() + ": " + ex.getMessage());
            order.setOrderStatus(OrderStatus.ERRORED);
            order.setErrorDescription(ex.getMessage());
            throw ex;
        }

        return order;
    }

    private void advance(Order order) {
        OrderStatus status = order.getOrderStatus();
        LOGGER.fine("Advancing order " + order.getOrderID() + " from status: " + status);

        try {
            switch (status) {
                case PLANNED:
                    LOGGER.info("Executing PLANNED->VALIDATED transition for order: " + order.getOrderID());
                    orderManager.validate(order);
                    order.setOrderStatus(OrderStatus.VALIDATED);
                    break;
                case VALIDATED:
                    LOGGER.info("Executing VALIDATED->ENRICHED transition for order: " + order.getOrderID());
                    orderManager.enrich(order);
                    order.setOrderStatus(OrderStatus.ENRICHED);
                    break;
                case ENRICHED:
                    LOGGER.info("Executing ENRICHED->PLACED transition for order: " + order.getOrderID());
                    orderManager.place(order);
                    order.setOrderStatus(OrderStatus.PLACED);
                    break;
                case PLACED:
                    LOGGER.info("Order " + order.getOrderID() + " is PLACED and awaiting batchout to become BULKED");
                    break;
                case BULKED:
                    LOGGER.info("Executing BULKED->CONFIRMED transition for order: " + order.getOrderID());
                    orderManager.confirm(order);
                    order.setOrderStatus(OrderStatus.CONFIRMED);
                    break;
                case CONFIRMED:
                    LOGGER.info("Executing CONFIRMED->CONTRACTED transition for order: " + order.getOrderID());
                    orderManager.contract(order);
                    order.setOrderStatus(OrderStatus.BOOKED);
                    break;
                case ERRORED:
                    break;
                default:
                    throw new IllegalStateException("Unsupported order status: " + status);
            }
        } catch (RuntimeException ex) {
            LOGGER.severe("Error during " + status + " transition for order " + order.getOrderID() + ": " + ex.getMessage());
            order.setOrderStatus(OrderStatus.ERRORED);
            order.setErrorDescription(ex.getMessage());
            throw ex;
        }
    }

    private boolean isOmsInternal(OrderStatus status) {
        return status == OrderStatus.PLANNED || status == OrderStatus.ENRICHED || 
            status == OrderStatus.VALIDATED || status == OrderStatus.BULKED;
    }
}
