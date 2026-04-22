package com.iiit.oms.transfer;

import com.iiit.oms.model.BulkOrder;

/**
 * Abstraction for communicating with a Transfer Agent (NSCC or RBC).
 */
public interface TransferAgentClient {
    /**
     * Transmits a bulk order to the transfer agent.
     * @param bulkOrder the bulk order to transmit
     * @return acknowledgement from the transfer agent
     */
    TransmissionAck transmit(BulkOrder bulkOrder);

    /**
     * Returns the name of this transfer agent ("NSCC" or "RBC").
     */
    String getName();
}
