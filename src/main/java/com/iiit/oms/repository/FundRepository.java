package com.iiit.oms.repository;

import com.iiit.oms.model.Fund;

import java.util.List;
import java.util.Optional;

public interface FundRepository {
    Fund save(Fund fund);

    Optional<Fund> findByFundId(String fundID);

    List<Fund> findAll();

    boolean existsByFundId(String fundID);

    void deleteByFundId(String fundID);
}