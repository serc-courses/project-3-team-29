package com.iiit.oms.repository.postgres;

import com.iiit.oms.db.postgres.PostgresReconciliationBreakDatabase;
import com.iiit.oms.model.ReconciliationBreak;
import com.iiit.oms.repository.ReconciliationBreakRepository;

import java.util.List;
import java.util.Objects;

public class PostgresReconciliationBreakRepository implements ReconciliationBreakRepository {
    private final PostgresReconciliationBreakDatabase database;

    public PostgresReconciliationBreakRepository(PostgresReconciliationBreakDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public void save(ReconciliationBreak breakRecord) {
        database.insert(breakRecord);
    }

    @Override
    public List<ReconciliationBreak> findAll() {
        return database.findAll();
    }

    @Override
    public List<ReconciliationBreak> findUnresolved() {
        return database.findUnresolved();
    }

    @Override
    public void markEscalated(String breakId) {
        database.markEscalated(breakId);
    }

    @Override
    public void markResolved(String breakId) {
        database.markResolved(breakId);
    }

    /**
     * Delegate for the escalation scheduler — find unresolved breaks older than
     * given seconds.
     */
    public List<ReconciliationBreak> findUnresolvedOlderThan(long seconds) {
        return database.findUnresolvedOlderThan(seconds);
    }
}
