package com.iiit.oms.repository.postgres;

import com.iiit.oms.db.postgres.PostgresAccountDatabase;
import com.iiit.oms.model.Account;
import com.iiit.oms.repository.AccountRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PostgresAccountRepository implements AccountRepository {
    private final PostgresAccountDatabase database;

    public PostgresAccountRepository(PostgresAccountDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public Account save(Account account) {
        Objects.requireNonNull(account, "account must not be null");
        Objects.requireNonNull(account.getAccountID(), "accountID must not be null");
        database.upsert(account);
        return account;
    }

    @Override
    public Optional<Account> findByAccountId(String accountID) {
        return database.getById(accountID);
    }

    @Override
    public List<Account> findAll() {
        return database.getAll();
    }

    @Override
    public boolean existsByAccountId(String accountID) {
        return database.exists(accountID);
    }

    @Override
    public void deleteByAccountId(String accountID) {
        database.deleteById(accountID);
    }
}
