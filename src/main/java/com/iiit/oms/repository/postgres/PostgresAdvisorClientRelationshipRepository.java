package com.iiit.oms.repository.postgres;

import com.iiit.oms.db.postgres.PostgresAdvisorClientRelationshipDatabase;
import com.iiit.oms.model.AdvisorClientRelationship;
import com.iiit.oms.repository.AdvisorClientRelationshipRepository;
import java.util.List;

public class PostgresAdvisorClientRelationshipRepository implements AdvisorClientRelationshipRepository {
    private final PostgresAdvisorClientRelationshipDatabase db;

    public PostgresAdvisorClientRelationshipRepository(PostgresAdvisorClientRelationshipDatabase db) {
        this.db = db;
    }

    @Override
    public AdvisorClientRelationship save(AdvisorClientRelationship rel) { return db.save(rel); }

    @Override
    public List<String> findClientAccountIds(String advisorID) { return db.findClientAccountIds(advisorID); }

    @Override
    public List<String> findAdvisorsByAccountId(String accountID) { return db.findAdvisorsByAccountId(accountID); }

    @Override
    public boolean isClientOfAdvisor(String advisorID, String accountID) { return db.isClientOfAdvisor(advisorID, accountID); }

    @Override
    public List<AdvisorClientRelationship> findAll() { return db.findAll(); }
}
