package com.iiit.oms.util;

import com.iiit.oms.model.Advisor;
import com.iiit.oms.model.AdvisorClientRelationship;
import com.iiit.oms.repository.AdvisorClientRelationshipRepository;
import com.iiit.oms.repository.AdvisorRepository;

public final class AdvisorMockDataUtil {
    private AdvisorMockDataUtil() {}

    public static void seedIfMissing(AdvisorRepository advisorRepo,
                                     AdvisorClientRelationshipRepository relationshipRepo) {
        if (!advisorRepo.findAll().isEmpty()) return;

        advisorRepo.save(new Advisor("ADV001", "Advisor One", "adv001@oms.local"));
        advisorRepo.save(new Advisor("ADV002", "Advisor Two", "adv002@oms.local"));

        // ADV001 manages ACCT00001 – ACCT00005
        for (int i = 1; i <= 5; i++) {
            String accountID = String.format("ACCT%05d", i);
            relationshipRepo.save(new AdvisorClientRelationship("ADV001", accountID, "ACTIVE"));
        }
        // ADV002 manages ACCT00006 – ACCT00010
        for (int i = 6; i <= 10; i++) {
            String accountID = String.format("ACCT%05d", i);
            relationshipRepo.save(new AdvisorClientRelationship("ADV002", accountID, "ACTIVE"));
        }
    }
}
