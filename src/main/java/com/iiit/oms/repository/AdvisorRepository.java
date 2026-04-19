package com.iiit.oms.repository;

import com.iiit.oms.model.Advisor;
import java.util.List;
import java.util.Optional;

public interface AdvisorRepository {
    Advisor save(Advisor advisor);
    Optional<Advisor> findByAdvisorId(String advisorID);
    List<Advisor> findAll();
}
