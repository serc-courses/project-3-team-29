package com.iiit.oms.repository.inmemory;

import com.iiit.oms.db.inmemory.InMemoryFundDatabase;
import com.iiit.oms.model.Fund;
import com.iiit.oms.repository.FundRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InMemoryFundRepository implements FundRepository {
    private final InMemoryFundDatabase database;

    public InMemoryFundRepository(InMemoryFundDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public Fund save(Fund fund) {
        Objects.requireNonNull(fund, "fund must not be null");
        Objects.requireNonNull(fund.getFundID(), "fundID must not be null");
        database.upsert(fund);
        return fund;
    }

    @Override
    public Optional<Fund> findByFundId(String fundID) {
        return database.getById(fundID);
    }

    @Override
    public List<Fund> findAll() {
        return database.getAll();
    }

    @Override
    public boolean existsByFundId(String fundID) {
        return database.exists(fundID);
    }

    @Override
    public void deleteByFundId(String fundID) {
        database.deleteById(fundID);
    }
}