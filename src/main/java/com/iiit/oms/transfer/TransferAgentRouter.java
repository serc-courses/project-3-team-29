package com.iiit.oms.transfer;

import com.iiit.oms.model.BulkOrder;

import java.util.logging.Logger;

/**
 * Routes bulk orders to the correct Transfer Agent (NSCC or RBC)
 * based on the fund's offshore flag.
 */
public class TransferAgentRouter {
    private static final Logger LOGGER = Logger.getLogger(TransferAgentRouter.class.getName());

    private final NsccSimulator nscc;
    private final RbcSimulator rbc;

    public TransferAgentRouter() {
        this.nscc = new NsccSimulator();
        this.rbc = new RbcSimulator();
    }

    /**
     * Transmits a bulk order to the appropriate TA based on transferAgent field.
     * If transferAgent is "RBC", routes to RBC simulator; otherwise routes to NSCC.
     */
    public TransmissionAck transmit(BulkOrder bulkOrder) {
        String ta = bulkOrder.getTransferAgent();
        TransferAgentClient client = "RBC".equalsIgnoreCase(ta) ? rbc : nscc;
        LOGGER.info("Routing bulk order " + bulkOrder.getOrderID() + " to " + client.getName());
        return client.transmit(bulkOrder);
    }

    /**
     * Transmits using an explicit TA name override (e.g., derived from fund.isOffshore()).
     */
    public TransmissionAck transmit(BulkOrder bulkOrder, String transferAgentName) {
        TransferAgentClient client = "RBC".equalsIgnoreCase(transferAgentName) ? rbc : nscc;
        LOGGER.info("Routing bulk order " + bulkOrder.getOrderID() + " to " + client.getName() + " (explicit)");
        return client.transmit(bulkOrder);
    }
}
