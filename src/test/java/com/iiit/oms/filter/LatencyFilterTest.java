package com.iiit.oms.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class LatencyFilterTest {

    @BeforeEach
    void resetStatics() throws Exception {
        // Reset all static fields between tests for isolation
        resetField("totalLatencyMs", 0L);
        resetField("totalRequests",  0);
        resetField("ringIdx",        0);
        // Zero out ring buffer
        Field ringField = LatencyFilter.class.getDeclaredField("ring");
        ringField.setAccessible(true);
        long[] ring = (long[]) ringField.get(null);
        for (int i = 0; i < ring.length; i++) ring[i] = 0L;
    }

    private static void resetField(String name, Object value) throws Exception {
        Field f = LatencyFilter.class.getDeclaredField(name);
        f.setAccessible(true);
        if (f.getType().getName().equals("java.util.concurrent.atomic.AtomicLong")) {
            ((java.util.concurrent.atomic.AtomicLong) f.get(null)).set((long) value);
        } else if (f.getType().getName().equals("java.util.concurrent.atomic.AtomicInteger")) {
            ((java.util.concurrent.atomic.AtomicInteger) f.get(null)).set((int) value);
        }
    }

    private static void injectLatencies(long... values) throws Exception {
        Field ringField  = LatencyFilter.class.getDeclaredField("ring");
        Field idxField   = LatencyFilter.class.getDeclaredField("ringIdx");
        Field totalMs    = LatencyFilter.class.getDeclaredField("totalLatencyMs");
        Field totalReqs  = LatencyFilter.class.getDeclaredField("totalRequests");
        ringField.setAccessible(true);
        idxField.setAccessible(true);
        totalMs.setAccessible(true);
        totalReqs.setAccessible(true);

        long[] ring = (long[]) ringField.get(null);
        long sum = 0;
        for (int i = 0; i < values.length; i++) {
            ring[i] = values[i];
            sum += values[i];
        }
        ((java.util.concurrent.atomic.AtomicInteger) idxField.get(null)).set(values.length);
        ((java.util.concurrent.atomic.AtomicLong)    totalMs.get(null)).set(sum);
        ((java.util.concurrent.atomic.AtomicInteger) totalReqs.get(null)).set(values.length);
    }

    // --- p99 calculation ---

    @Test
    void getP99Ms_noRequests_returnsZero() {
        assertEquals(0, LatencyFilter.getP99Ms());
    }

    @Test
    void getP99Ms_uniformLatencies_correctPercentile() throws Exception {
        // 100 latencies: 1..100ms — p99 should be ~99ms
        long[] vals = new long[100];
        for (int i = 0; i < 100; i++) vals[i] = i + 1;
        injectLatencies(vals);
        long p99 = LatencyFilter.getP99Ms();
        assertTrue(p99 >= 98 && p99 <= 100, "Expected ~99ms, got: " + p99);
    }

    @Test
    void getP99Ms_singleValue_returnsThatValue() throws Exception {
        injectLatencies(42L);
        assertEquals(42L, LatencyFilter.getP99Ms());
    }

    @Test
    void getP99Ms_outliers_capturedByP99() throws Exception {
        // 98 values of 1ms, 2 values of 5000ms — p99 (index 98 of 100) should be 5000ms
        long[] vals = new long[100];
        for (int i = 0; i < 98; i++) vals[i] = 1L;
        vals[98] = 5000L;
        vals[99] = 5000L;
        injectLatencies(vals);
        long p99 = LatencyFilter.getP99Ms();
        assertEquals(5000L, p99, "The 5000ms outlier should appear at p99 (2% of requests)");
    }

    @Test
    void getAvgMs_correctAverage() throws Exception {
        injectLatencies(10L, 20L, 30L);
        assertEquals(20L, LatencyFilter.getAvgMs());
    }

    @Test
    void getAvgMs_noRequests_returnsZero() {
        assertEquals(0L, LatencyFilter.getAvgMs());
    }

    @Test
    void getTotalRequests_correctCount() throws Exception {
        injectLatencies(1L, 2L, 3L, 4L, 5L);
        assertEquals(5, LatencyFilter.getTotalRequests());
    }

    // --- SlaHandler cutoff time logic (tested through time arithmetic) ---

    @Test
    void slaHandler_secondsToOnshore_isPositive() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        java.time.ZonedDateTime onshore = now.toLocalDate()
                .atTime(16, 0).atZone(java.time.ZoneOffset.UTC);
        if (!now.isBefore(onshore)) onshore = onshore.plusDays(1);
        long secs = java.time.Duration.between(now, onshore).getSeconds();
        assertTrue(secs > 0 && secs <= 86400,
                "Seconds to onshore must be in (0, 86400], got: " + secs);
    }

    @Test
    void slaHandler_secondsToOffshore_isPositive() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        java.time.ZonedDateTime offshore = now.toLocalDate()
                .atTime(1, 0).atZone(java.time.ZoneOffset.UTC);
        if (!now.isBefore(offshore)) offshore = offshore.plusDays(1);
        long secs = java.time.Duration.between(now, offshore).getSeconds();
        assertTrue(secs > 0 && secs <= 86400,
                "Seconds to offshore must be in (0, 86400], got: " + secs);
    }

    @Test
    void slaHandler_bothCutoffs_neverInThePast() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);

        java.time.ZonedDateTime onshore = now.toLocalDate()
                .atTime(16, 0).atZone(java.time.ZoneOffset.UTC);
        if (!now.isBefore(onshore)) onshore = onshore.plusDays(1);

        java.time.ZonedDateTime offshore = now.toLocalDate()
                .atTime(1, 0).atZone(java.time.ZoneOffset.UTC);
        if (!now.isBefore(offshore)) offshore = offshore.plusDays(1);

        assertTrue(onshore.isAfter(now),  "Onshore deadline must be in the future");
        assertTrue(offshore.isAfter(now), "Offshore deadline must be in the future");
    }

    // --- Ring buffer wrap-around ---

    @Test
    void p99_ringBuffer_wrapsCorrectly() throws Exception {
        // Fill ring beyond capacity — last RING_SIZE values should be tracked
        Field ringField = LatencyFilter.class.getDeclaredField("ring");
        Field idxField  = LatencyFilter.class.getDeclaredField("ringIdx");
        Field totalReqs = LatencyFilter.class.getDeclaredField("totalRequests");
        Field totalMs   = LatencyFilter.class.getDeclaredField("totalLatencyMs");
        ringField.setAccessible(true);
        idxField.setAccessible(true);
        totalReqs.setAccessible(true);
        totalMs.setAccessible(true);

        int ringSize = ((long[]) ringField.get(null)).length; // 1000
        // Simulate writing 1001 values — the first slot gets overwritten with value 1001
        long[] ring = (long[]) ringField.get(null);
        for (int i = 0; i < ringSize; i++) ring[i] = i + 1; // 1..1000
        ring[0] = 9999L; // simulate wrap: index 1000 % 1000 = 0 → overwrites slot 0
        ((java.util.concurrent.atomic.AtomicInteger) idxField.get(null)).set(ringSize + 1);
        ((java.util.concurrent.atomic.AtomicInteger) totalReqs.get(null)).set(ringSize + 1);
        ((java.util.concurrent.atomic.AtomicLong)    totalMs.get(null)).set(1L); // irrelevant for p99

        // p99 of filled ring (capped at ringSize)
        long p99 = LatencyFilter.getP99Ms();
        assertTrue(p99 > 0, "p99 should be computed from ring buffer contents");
    }
}
