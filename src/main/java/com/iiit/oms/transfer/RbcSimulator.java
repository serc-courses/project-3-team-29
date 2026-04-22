package com.iiit.oms.transfer;

import com.iiit.oms.model.BulkOrder;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Simulates the RBC (Royal Bank of Canada) transfer agent for offshore/international funds.
 * In production, this would be replaced by an RBC FTP/SWIFT integration.
 * For offshore mutual fund orders (isOffshore = true).
 */
public class RbcSimulator implements TransferAgentClient {
    private static final Logger LOGGER = Logger.getLogger(RbcSimulator.class.getName());

    @Override
    public TransmissionAck transmit(BulkOrder bulkOrder) {
        String refNumber = "RBC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOGGER.info("[RBC-SIM] Transmitting bulk order " + bulkOrder.getOrderID()
                + " qty=" + bulkOrder.getQuantity()
                + " amt=" + bulkOrder.getAmount()
                + " → assigned ref=" + refNumber);

        return new TransmissionAck(refNumber, "ACCEPTED", "RBC",
                "Bulk order accepted by RBC simulator. Ref: " + refNumber);
    }

    @Override
    public String getName() {
        return "RBC";
    }
}
