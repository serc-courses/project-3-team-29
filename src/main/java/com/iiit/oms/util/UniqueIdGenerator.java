package com.iiit.oms.util;

import java.util.Objects;
import java.util.UUID;

public final class UniqueIdGenerator {
    private UniqueIdGenerator() {
    }

    public static String generate(String prefix) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        String trimmedPrefix = prefix.trim();
        if (trimmedPrefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }

        String normalizedPrefix = trimmedPrefix.toUpperCase();
        String randomPart = UUID.randomUUID().toString().replace("-", "");
        return normalizedPrefix + "-" + randomPart;
    }
}
