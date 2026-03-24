package com.iiit.oms.db.inmemory;

import com.iiit.oms.model.Fund;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFundDatabase {
    private final Map<String, Fund> funds = new ConcurrentHashMap<>();

    public void upsert(Fund fund) {
        funds.put(fund.getFundID(), fund);
    }

    public Optional<Fund> getById(String fundID) {
        return Optional.ofNullable(funds.get(fundID));
    }

    public List<Fund> getAll() {
        return new ArrayList<>(funds.values());
    }

    public boolean exists(String fundID) {
        return funds.containsKey(fundID);
    }

    public void deleteById(String fundID) {
        funds.remove(fundID);
    }

    public void clear() {
        funds.clear();
    }
}