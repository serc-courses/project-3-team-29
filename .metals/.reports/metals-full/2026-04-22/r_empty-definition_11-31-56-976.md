error id: file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/OrderStateMachine.java:com/iiit/oms/model/OrderStatus#
file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/OrderStateMachine.java
empty definition using pc, found symbol in pc: com/iiit/oms/model/OrderStatus#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 137
uri: file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/OrderStateMachine.java
text:
```scala
package com.iiit.oms.processor;

import com.iiit.oms.model.AuditLogEntry;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.@@OrderStatus;
import com.iiit.oms.repository.AuditLogRepository;

import java.util.Objects;
import java.util.logging.Logger;

public class OrderStateMachine {
    private static final Logger LOGGER = Logger.getLogger(OrderStateMachine.class.getName());
    private final OrderManager orderManager;
    private AuditLogRepository auditLogRepository;

    public OrderStateMachine(OrderManager orderManager) {
        this.orderManager = Objects.requireNonNull(orderManager, "orderManager must not be null");
    }

    public void setAuditLogRepository(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
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

    /**
     * Advance order from CONFIRMED → CONTRACTED (once contract callback received with NAV/shares).
     * Called by the ContractCallbackHandler in the REST server.
     */
    public void advanceToContracted(Order order, String contractRef, java.math.BigDecimal nav, java.math.BigDecimal allocatedShares) {
        if (order.getOrderStatus() != OrderStatus.TRANSMITTED) {
            throw new IllegalStateException("Cannot contract order " + order.getOrderID()
                    + " — expected TRANSMITTED but was " + order.getOrderStatus());
        }
        OrderStatus from = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.CONTRACTED);
        order.setContractRef(contractRef);
        order.setNav(nav);
        order.setAllocatedShares(allocatedShares);
        writeAuditLog(order.getOrderID(), from, OrderStatus.CONTRACTED, "ContractCallbackHandler",
                "contractRef=" + contractRef + ", nav=" + nav + ", allocatedShares=" + allocatedShares);
    }

    /**
     * Advance order from CONTRACTED → BOOKED.
     */
    public void advanceToBooked(Order order) {
        OrderStatus from = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.BOOKED);
        writeAuditLog(order.getOrderID(), from, OrderStatus.BOOKED, "ContractCallbackHandler", null);
    }

    private void advance(Order order) {
        OrderStatus status = order.getOrderStatus();
        OrderStatus nextStatus = null;
        LOGGER.fine("Advancing order " + order.getOrderID() + " from status: " + status);

        try {
            switch (status) {
                case PLANNED:
                    LOGGER.info("Executing PLANNED->VALIDATED transition for order: " + order.getOrderID());
                    orderManager.validate(order);
                    nextStatus = OrderStatus.VALIDATED;
                    break;
                case VALIDATED:
                    LOGGER.info("Executing VALIDATED->ENRICHED transition for order: " + order.getOrderID());
                    orderManager.enrich(order);
                    nextStatus = OrderStatus.ENRICHED;
                    break;
                case ENRICHED:
                    LOGGER.info("Executing ENRICHED->PLACED transition for order: " + order.getOrderID());
                    orderManager.place(order);
                    nextStatus = OrderStatus.PLACED;
                    break;
                case PLACED:
                    LOGGER.info("Order " + order.getOrderID() + " is PLACED and awaiting batchout to become BULKED");
                    return; // stop loop; BatchoutScheduler handles PLACED→BULKED→TRANSMITTED
                case ERRORED:
                    return;
                default:
                    throw new IllegalStateException("Unsupported order status for auto-processing: " + status);
            }
            order.setOrderStatus(nextStatus);
            writeAuditLog(order.getOrderID(), status, nextStatus, "OrderStateMachine", null);
        } catch (RuntimeException ex) {
            LOGGER.severe("Error during " + status + " transition for order " + order.getOrderID() + ": " + ex.getMessage());
            order.setOrderStatus(OrderStatus.ERRORED);
            order.setErrorDescription(ex.getMessage());
            writeAuditLog(order.getOrderID(), status, OrderStatus.ERRORED, "OrderStateMachine", ex.getMessage());
            throw ex;
        }
    }

    private boolean isOmsInternal(OrderStatus status) {
        // BULKED is intentionally NOT included here.
        // BatchoutScheduler is responsible for the PLACED→BULKED→TRANSMITTED transitions.
        return status == OrderStatus.PLANNED
 status == OrderStatus.VALIDATED
 status == OrderStatus.ENRICHED;
    }

    private void writeAuditLog(String orderID, OrderStatus from, OrderStatus to, String actor, String details) {
        if (auditLogRepository == null) return;
        try {
            String fromStr = from != null ? from.name() : null;
            auditLogRepository.log(new AuditLogEntry(orderID, fromStr, to.name(), actor, details));
        } catch (Exception ex) {
            LOGGER.warning("Failed to write audit log for order " + orderID + ": " + ex.getMessage());
        }
    }
}


```


#### Short summary: 

empty definition using pc, found symbol in pc: com/iiit/oms/model/OrderStatus#