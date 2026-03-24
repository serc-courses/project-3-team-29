package com.iiit.oms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueIdGeneratorTest {

    @Test
    void shouldGenerateIdWithProvidedPrefix() {
        String generatedId = UniqueIdGenerator.generate("ORD");

        assertTrue(generatedId.startsWith("ORD-"));
    }

    @Test
    void shouldNormalizePrefixToUppercaseAndTrimSpaces() {
        String generatedId = UniqueIdGenerator.generate("  blk  ");

        assertTrue(generatedId.startsWith("BLK-"));
    }

    @Test
    void shouldGenerateUniqueIds() {
        String id1 = UniqueIdGenerator.generate("ORD");
        String id2 = UniqueIdGenerator.generate("ORD");

        assertNotEquals(id1, id2);
    }

    @Test
    void shouldFailWhenPrefixIsNullOrBlank() {
        assertThrows(NullPointerException.class, () -> UniqueIdGenerator.generate(null));
        assertThrows(IllegalArgumentException.class, () -> UniqueIdGenerator.generate("   "));
    }
}
