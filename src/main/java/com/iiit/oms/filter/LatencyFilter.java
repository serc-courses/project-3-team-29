package com.iiit.oms.filter;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class LatencyFilter extends Filter {
    private static final Logger LOGGER = Logger.getLogger(LatencyFilter.class.getName());
    private static final int RING_SIZE = 1000;

    private static final AtomicLong totalLatencyMs  = new AtomicLong(0);
    private static final AtomicInteger totalRequests = new AtomicInteger(0);

    // Ring buffer for p99 calculation — index wraps modulo RING_SIZE
    private static final long[] ring     = new long[RING_SIZE];
    private static final AtomicInteger ringIdx = new AtomicInteger(0);

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(exchange);
        } finally {
            long duration = System.currentTimeMillis() - start;
            totalLatencyMs.addAndGet(duration);
            totalRequests.incrementAndGet();

            // Write into ring buffer (wraps at RING_SIZE)
            ring[ringIdx.getAndIncrement() % RING_SIZE] = duration;

            if (duration > 500) {
                LOGGER.warning(String.format("HIGH API LATENCY [%s]: %s %s took %d ms",
                        Thread.currentThread().getName(),
                        exchange.getRequestMethod(),
                        exchange.getRequestURI(),
                        duration));
            }
        }
    }

    @Override
    public String description() {
        return "Tracks API latency for SLA compliance (avg + p99)";
    }

    public static long getAvgMs() {
        int reqs = totalRequests.get();
        return reqs == 0 ? 0 : totalLatencyMs.get() / reqs;
    }

    /** @deprecated Use {@link #getAvgMs()} */
    @Deprecated
    public static long getAverageLatency() { return getAvgMs(); }

    public static int getTotalRequests() { return totalRequests.get(); }

    public static long getP99Ms() {
        // Snapshot the ring buffer — only consider populated slots
        int filled = Math.min(totalRequests.get(), RING_SIZE);
        if (filled == 0) return 0;
        long[] snapshot = Arrays.copyOf(ring, filled);
        Arrays.sort(snapshot);
        int idx = (int) Math.ceil(filled * 0.99) - 1;
        return snapshot[Math.max(0, Math.min(idx, filled - 1))];
    }
}
