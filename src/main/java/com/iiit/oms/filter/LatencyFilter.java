package com.iiit.oms.filter;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class LatencyFilter extends Filter {
    private static final Logger LOGGER = Logger.getLogger(LatencyFilter.class.getName());
    
    // Simplistic moving average/total store for demonstration. 
    // In production, we'd use a histogram for p99.
    private static final AtomicLong totalLatencyMs = new AtomicLong(0);
    private static final AtomicInteger totalRequests = new AtomicInteger(0);

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(exchange);
        } finally {
            long duration = System.currentTimeMillis() - start;
            totalLatencyMs.addAndGet(duration);
            totalRequests.incrementAndGet();
            
            // Log SLAs running over 500ms as per NFR
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
        return "Tracks API Latency for SLA compliance";
    }

    public static long getAverageLatency() {
        int reqs = totalRequests.get();
        if (reqs == 0) return 0;
        return totalLatencyMs.get() / reqs;
    }
}
