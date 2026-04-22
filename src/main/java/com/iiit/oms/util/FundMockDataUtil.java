package com.iiit.oms.util;

import com.iiit.oms.model.Fund;
import com.iiit.oms.repository.FundRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FundMockDataUtil {
    private static final String[] SECTORS = {
            "Technology",
            "Healthcare",
            "Financial Services",
            "Energy",
            "Consumer Staples",
            "Consumer Discretionary",
            "Industrial",
            "Utilities",
            "Real Estate",
            "Telecommunications"
    };

    private FundMockDataUtil() {
    }

    public static List<Fund> insertMockFunds(FundRepository repository) {
        Objects.requireNonNull(repository, "repository must not be null");

        List<Fund> inserted = new ArrayList<>();
        int index = 1;
        for (String sector : SECTORS) {
            for (int i = 1; i <= 5; i++) {
                String fundID = String.format("FND%03d", index);
                String fundName = "CharlesSchwabb " + sector + " Mutual Fund " + i;
                BigDecimal nav = BigDecimal.valueOf(10 + (index * 1.37)).setScale(2, RoundingMode.HALF_UP);
                Fund fund = new Fund(fundID, fundName, "CharlesSchwabb", nav);
                // FND041-FND050 (Telecommunications sector) are offshore funds → RBC routing
                fund.setOffshore(index >= 41 && index <= 50);
                inserted.add(repository.save(fund));
                index++;
            }
        }
        return inserted;
    }
}