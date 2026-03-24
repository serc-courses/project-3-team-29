package com.iiit.oms.repository;

import com.iiit.oms.model.BulkOrder;

import java.util.List;
import java.util.Optional;

public interface BulkOrderRepository {
    BulkOrder save(BulkOrder bulkOrder);

    Optional<BulkOrder> findByOrderId(String orderID);

    List<BulkOrder> findAll();

    boolean existsByOrderId(String orderID);

    void deleteByOrderId(String orderID);
}
