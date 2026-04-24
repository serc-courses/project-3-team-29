package com.iiit.oms.util;

import com.iiit.oms.model.User;
import com.iiit.oms.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Seeds demo users into an in-memory repository.
 *
 * Investor password: invest123
 * Advisor  password: advise123
 * Admin    password: admin123
 *
 * Each investor maps 1:1 to the account seeded by AccountMockDataUtil.
 * Each advisor maps to an existing ADV00x ID seeded by AdvisorMockDataUtil.
 *
 * Passwords are BCrypt-hashed before being stored.
 */
public final class UserSeedDataUtil {

    private static final String[][] INVESTORS = {
        // userID,     username,          plainPassword,  displayName,         accountID
        {"USR001", "john.miller",      "invest123", "John Miller",       "ACCT00001"},
        {"USR002", "emma.johnson",     "invest123", "Emma Johnson",      "ACCT00002"},
        {"USR003", "liam.davis",       "invest123", "Liam Davis",        "ACCT00003"},
        {"USR004", "olivia.brown",     "invest123", "Olivia Brown",      "ACCT00004"},
        {"USR005", "noah.wilson",      "invest123", "Noah Wilson",       "ACCT00005"},
        {"USR006", "ava.moore",        "invest123", "Ava Moore",         "ACCT00006"},
        {"USR007", "william.taylor",   "invest123", "William Taylor",    "ACCT00007"},
        {"USR008", "sophia.anderson",  "invest123", "Sophia Anderson",   "ACCT00008"},
        {"USR009", "james.thomas",     "invest123", "James Thomas",      "ACCT00009"},
        {"USR010", "isabella.jackson", "invest123", "Isabella Jackson",  "ACCT00010"},
    };

    private static final String[][] ADVISORS = {
        // userID,     username,    plainPassword,  displayName,    advisorID
        {"USR011", "advisor1", "advise123", "Advisor One", "ADV001"},
        {"USR012", "advisor2", "advise123", "Advisor Two", "ADV002"},
    };

    private static final String[][] ADMINS = {
        // userID,     username,  plainPassword, displayName
        {"USR013", "admin", "admin123", "OMS Admin"},
    };

    private UserSeedDataUtil() {}

    public static void seedIfMissing(UserRepository repo) {
        for (String[] r : INVESTORS) {
            ensureUser(repo, r[1], new User(
                    r[0], r[1], BCrypt.hashpw(r[2], BCrypt.gensalt()), "INVESTOR", r[3], r[4], null
            ));
        }
        for (String[] r : ADVISORS) {
            ensureUser(repo, r[1], new User(
                    r[0], r[1], BCrypt.hashpw(r[2], BCrypt.gensalt()), "ADVISOR", r[3], null, r[4]
            ));
        }
        for (String[] r : ADMINS) {
            ensureUser(repo, r[1], new User(
                    r[0], r[1], BCrypt.hashpw(r[2], BCrypt.gensalt()), "ADMIN", r[3], null, null
            ));
        }
    }

    private static void ensureUser(UserRepository repo, String username, User candidate) {
        if (repo.findByUsername(username).isPresent()) return;
        repo.save(candidate);
    }
}
