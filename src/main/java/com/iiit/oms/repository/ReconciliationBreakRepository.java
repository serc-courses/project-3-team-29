package com.iiit.oms.repository;

import com.iiit.oms.model.ReconciliationBreak;

import java.util.List;

/**
 * Repository for reconciliation break records.
 */
public interface ReconciliationBreakRepository {
    void save(ReconciliationBreak breakRecord);

    List<ReconciliationBreak> findAll();

    List<ReconciliationBreak> findUnresolved();

    void markEscalated(String breakId);

    void markResolved(String breakId);
}
