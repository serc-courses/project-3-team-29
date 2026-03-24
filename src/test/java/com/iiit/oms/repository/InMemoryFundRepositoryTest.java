package com.iiit.oms.repository;

import com.iiit.oms.db.inmemory.InMemoryFundDatabase;
import com.iiit.oms.model.Fund;
import com.iiit.oms.repository.inmemory.InMemoryFundRepository;
import com.iiit.oms.util.FundMockDataUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryFundRepositoryTest {

    @Test
    void insertMockFundsShouldStoreAndRetrieveFiftyFunds() {
        FundRepository repository = new InMemoryFundRepository(new InMemoryFundDatabase());

        List<Fund> inserted = FundMockDataUtil.insertMockFunds(repository);

        assertEquals(50, inserted.size());
        assertEquals(50, repository.findAll().size());
        assertTrue(repository.existsByFundId("FND001"));

        Optional<Fund> fund = repository.findByFundId("FND050");
        assertTrue(fund.isPresent());
        assertEquals("CharlesSchwabb Telecommunications Mutual Fund 5", fund.get().getFundName());
        assertEquals("CharlesSchwabb", fund.get().getFundFamily());
    }
}