package com.iiit.oms.repository;

import com.iiit.oms.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findByAccountId(String accountID);

    List<Account> findAll();

    boolean existsByAccountId(String accountID);

    void deleteByAccountId(String accountID);
}
