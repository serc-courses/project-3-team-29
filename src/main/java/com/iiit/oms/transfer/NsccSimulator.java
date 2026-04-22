package com.iiit.oms.transfer;

import com.iiit.oms.model.BulkOrder;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Simulates the NSCC (National Securities Clearing Corporation) transfer agent.
 * In production, this would be replaced by an NSCC FTP/SWIFT integration.
 * For domestic US mutual fund orders (isOffshore = false).
 */
public class NsccSimulator implements TransferAgentClient {
    private static final Logger LOGGER = Logger.getLogger(NsccSimulator.class.getName());

    @Override
    public TransmissionAck transmit(BulkOrder bulkOrder) {
        String refNumber = "NSCC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOGGER.info("[NSCC-SIM] Transmitting bulk order " + bulkOrder.getOrderID()
                + " qty=" + bulkOrder.getQuantity()
                + " amt=" + bulkOrder.getAmount()
                + " → assigned ref=" + refNumber);

        // Simulate a small processing delay (0 ms in test mode – just log)
        return new TransmissionAck(refNumber, "ACCEPTED", "NSCC",
                "Bulk order accepted by NSCC simulator. Ref: " + refNumber);
    }

    @Override
    public String getName() {
        return "NSCC";
    }
}
