package com.iiit.oms.repository;

import com.iiit.oms.model.AdvisorClientRelationship;
import java.util.List;

public interface AdvisorClientRelationshipRepository {
    AdvisorClientRelationship save(AdvisorClientRelationship relationship);
    List<String> findClientAccountIds(String advisorID);
    List<String> findAdvisorsByAccountId(String accountID);
    boolean isClientOfAdvisor(String advisorID, String accountID);
    List<AdvisorClientRelationship> findAll();
}
