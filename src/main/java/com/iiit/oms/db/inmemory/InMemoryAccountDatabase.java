package com.iiit.oms.db.inmemory;

import com.iiit.oms.model.Account;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountDatabase {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public void upsert(Account account) {
        accounts.put(account.getAccountID(), account);
    }

    public Optional<Account> getById(String accountID) {
        return Optional.ofNullable(accounts.get(accountID));
    }

    public List<Account> getAll() {
        return new ArrayList<>(accounts.values());
    }

    public boolean exists(String accountID) {
        return accounts.containsKey(accountID);
    }

    public void deleteById(String accountID) {
        accounts.remove(accountID);
    }

    public void clear() {
        accounts.clear();
    }
}
