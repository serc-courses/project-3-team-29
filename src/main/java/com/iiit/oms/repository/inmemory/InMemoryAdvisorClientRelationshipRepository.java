package com.iiit.oms.repository.inmemory;

import com.iiit.oms.model.AdvisorClientRelationship;
import com.iiit.oms.repository.AdvisorClientRelationshipRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryAdvisorClientRelationshipRepository implements AdvisorClientRelationshipRepository {
    private final Map<String, AdvisorClientRelationship> store = new ConcurrentHashMap<>();

    private static String key(String advisorID, String accountID) {
        return advisorID + "::" + accountID;
    }

    @Override
    public AdvisorClientRelationship save(AdvisorClientRelationship rel) {
        store.put(key(rel.getAdvisorID(), rel.getAccountID()), rel);
        return rel;
    }

    @Override
    public List<String> findClientAccountIds(String advisorID) {
        return store.values().stream()
                .filter(r -> advisorID.equals(r.getAdvisorID()))
                .map(AdvisorClientRelationship::getAccountID)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findAdvisorsByAccountId(String accountID) {
        return store.values().stream()
                .filter(r -> accountID.equals(r.getAccountID()))
                .map(AdvisorClientRelationship::getAdvisorID)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isClientOfAdvisor(String advisorID, String accountID) {
        return store.containsKey(key(advisorID, accountID));
    }

    @Override
    public List<AdvisorClientRelationship> findAll() {
        return new ArrayList<>(store.values());
    }
}
