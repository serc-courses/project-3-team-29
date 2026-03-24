package com.iiit.oms.util;

import com.iiit.oms.model.Account;
import com.iiit.oms.repository.AccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AccountMockDataUtil {
    private static final String[] US_MOCK_PERSON_NAMES = {
        "John Miller",
        "Emma Johnson",
        "Liam Davis",
        "Olivia Brown",
        "Noah Wilson",
        "Ava Moore",
        "William Taylor",
        "Sophia Anderson",
        "James Thomas",
        "Isabella Jackson"
    };

    private static final String[] MOCK_SSNS = {
        "201-55-1001",
        "202-55-1002",
        "203-55-1003",
        "204-55-1004",
        "205-55-1005",
        "206-55-1006",
        "207-55-1007",
        "208-55-1008",
        "209-55-1009",
        "210-55-1010"
    };

    private AccountMockDataUtil() {
    }

    public static List<Account> insertMockAccounts(AccountRepository repository) {
        Objects.requireNonNull(repository, "repository must not be null");

        List<Account> inserted = new ArrayList<>();
        for (int i = 0; i < US_MOCK_PERSON_NAMES.length; i++) {
            Account account = new Account(
                String.format("ACCT%05d", i + 1),
                US_MOCK_PERSON_NAMES[i],
                MOCK_SSNS[i]
            );
            inserted.add(repository.save(account));
        }
        return inserted;
    }
}
