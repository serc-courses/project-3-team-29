package com.iiit.oms.repository;

import com.iiit.oms.db.inmemory.InMemoryAccountDatabase;
import com.iiit.oms.model.Account;
import com.iiit.oms.repository.inmemory.InMemoryAccountRepository;
import com.iiit.oms.util.AccountMockDataUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAccountRepositoryTest {

    @Test
    void insertMockAccountsShouldStoreAndRetrieveTenAccounts() {
        AccountRepository repository = new InMemoryAccountRepository(new InMemoryAccountDatabase());

        List<Account> inserted = AccountMockDataUtil.insertMockAccounts(repository);

        assertEquals(10, inserted.size());
        assertEquals(10, repository.findAll().size());
        assertTrue(repository.existsByAccountId("ACCT00001"));

        Optional<Account> account = repository.findByAccountId("ACCT00010");
        assertTrue(account.isPresent());
        assertEquals("Isabella Jackson", account.get().getAccountName());
    }
}
