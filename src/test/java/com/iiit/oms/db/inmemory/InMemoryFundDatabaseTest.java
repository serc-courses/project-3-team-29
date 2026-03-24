package com.iiit.oms.db.inmemory;

import com.iiit.oms.model.Fund;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryFundDatabaseTest {

    @Test
    void shouldStoreAndRetrieveFunds() {
        InMemoryFundDatabase database = new InMemoryFundDatabase();

        database.upsert(new Fund("FND001", "CharlesSchwabb Technology Mutual Fund 1", "CharlesSchwabb", BigDecimal.valueOf(11.37)));

        List<Fund> funds = database.getAll();

        assertEquals(1, funds.size());
        assertTrue(database.exists("FND001"));
        assertTrue(funds.stream().allMatch(fund -> "CharlesSchwabb".equals(fund.getFundFamily())));
    }
}