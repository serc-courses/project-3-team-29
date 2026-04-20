package com.iiit.oms.repository.inmemory;

import com.iiit.oms.model.Advisor;
import com.iiit.oms.repository.AdvisorRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAdvisorRepository implements AdvisorRepository {
    private final Map<String, Advisor> store = new ConcurrentHashMap<>();

    @Override
    public Advisor save(Advisor advisor) {
        store.put(advisor.getAdvisorID(), advisor);
        return advisor;
    }

    @Override
    public Optional<Advisor> findByAdvisorId(String advisorID) {
        return Optional.ofNullable(store.get(advisorID));
    }

    @Override
    public List<Advisor> findAll() {
        return new ArrayList<>(store.values());
    }
}
