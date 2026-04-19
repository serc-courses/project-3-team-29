package com.iiit.oms.repository.postgres;

import com.iiit.oms.db.postgres.PostgresAdvisorDatabase;
import com.iiit.oms.model.Advisor;
import com.iiit.oms.repository.AdvisorRepository;
import java.util.List;
import java.util.Optional;

public class PostgresAdvisorRepository implements AdvisorRepository {
    private final PostgresAdvisorDatabase db;

    public PostgresAdvisorRepository(PostgresAdvisorDatabase db) {
        this.db = db;
    }

    @Override
    public Advisor save(Advisor advisor) { return db.save(advisor); }

    @Override
    public Optional<Advisor> findByAdvisorId(String advisorID) { return db.findByAdvisorId(advisorID); }

    @Override
    public List<Advisor> findAll() { return db.findAll(); }
}
