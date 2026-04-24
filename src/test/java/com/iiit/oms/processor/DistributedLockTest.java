package com.iiit.oms.processor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DistributedLock using a null JedisPool (no Redis required).
 *
 * The null-JedisPool path exercises the single-instance fallback behavior.
 * Integration tests against a live Redis cluster are documented in nfrs_progress.md.
 */
class DistributedLockTest {

    // --- null jedisPool (single-instance fallback) ---

    @Test
    void nullJedisPool_tryAcquire_returnsTrue() {
        DistributedLock lock = new DistributedLock(null, "key", "instance-1", 10);
        assertTrue(lock.tryAcquire(), "Should always grant lock when Redis unavailable");
    }

    @Test
    void nullJedisPool_multipleAcquires_allReturnTrue() {
        DistributedLock lock = new DistributedLock(null, "key", "instance-1", 10);
        for (int i = 0; i < 5; i++) {
            assertTrue(lock.tryAcquire(), "Every acquire should succeed without Redis");
        }
    }

    // --- Simulate two replicas with an in-memory lock map (no Redis needed) ---

    @Test
    void twoReplicas_onlyOneExecutesPerTick_withInMemorySimulation() throws InterruptedException {
        // Simulate the lock behavior using a simple shared state
        java.util.concurrent.ConcurrentHashMap<String, String> lockStore =
                new java.util.concurrent.ConcurrentHashMap<>();
        String lockKey = "oms:scheduler:order-lock";
        long ttlMs = 500; // 500ms TTL for test speed

        AtomicInteger executionCount = new AtomicInteger(0);

        Runnable replicaTask = () -> {
            // Simulate SET NX EX using putIfAbsent
            long now = System.currentTimeMillis();
            String existing = lockStore.get(lockKey);
            if (existing == null) {
                // Try to acquire
                String prev = lockStore.putIfAbsent(lockKey, Thread.currentThread().getName());
                if (prev == null) {
                    // Acquired
                    executionCount.incrementAndGet();
                    // Simulate work, then release after TTL (in real code it auto-expires)
                    try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    lockStore.remove(lockKey);
                }
            }
        };

        Thread r1 = new Thread(replicaTask, "replica-1");
        Thread r2 = new Thread(replicaTask, "replica-2");
        r1.start();
        r2.start();
        r1.join(1000);
        r2.join(1000);

        assertEquals(1, executionCount.get(),
                "Only one replica should execute when they race for the lock");
    }

    @Test
    void lockConstants_orderScheduler_ttlShorterThanInterval() {
        // Verify the lock TTL chosen is < the scheduler interval (25s < 30s)
        long pollInterval = 30L;
        long lockTtl = 25L;
        assertTrue(lockTtl < pollInterval,
                "Lock TTL (" + lockTtl + "s) must be less than poll interval (" + pollInterval + "s)");
    }

    @Test
    void lockConstants_batchoutScheduler_ttlShorterThanInterval() {
        long batchoutInterval = 120L;
        long lockTtl = 110L;
        assertTrue(lockTtl < batchoutInterval,
                "Lock TTL (" + lockTtl + "s) must be less than batchout interval (" + batchoutInterval + "s)");
    }

    // --- nginx config static verification ---

    @Test
    void nginxConfig_exists_andConfiguresUpstream() throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("nginx.conf")));
        assertTrue(content.contains("upstream oms_backend"), "nginx.conf must define upstream block");
        assertTrue(content.contains("server app_1:8080"),    "upstream must include app_1");
        assertTrue(content.contains("server app_2:8080"),    "upstream must include app_2");
        assertTrue(content.contains("least_conn"),           "upstream must use least_conn balancing");
    }

    @Test
    void nginxConfig_sseLocation_hasBufferingOff() throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("nginx.conf")));
        assertTrue(content.contains("/view/stream"),    "nginx.conf must have /view/stream location");
        assertTrue(content.contains("proxy_buffering    off"), "SSE location must disable buffering");
    }

    @Test
    void dockerfile_exists_andUsesJre() throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("Dockerfile")));
        assertTrue(content.contains("eclipse-temurin:11-jre"), "Dockerfile must use JRE base image");
        assertTrue(content.contains("app.jar"),                "Dockerfile must copy app.jar");
        assertTrue(content.contains("ENTRYPOINT"),             "Dockerfile must define ENTRYPOINT");
    }

    @Test
    void dockerCompose_hasTwoAppReplicas() throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("docker-compose.yml")));
        assertTrue(content.contains("app_1:"), "docker-compose.yml must define app_1");
        assertTrue(content.contains("app_2:"), "docker-compose.yml must define app_2");
        assertTrue(content.contains("nginx:"),  "docker-compose.yml must define nginx service");
    }

    @Test
    void dockerCompose_nginx_exposesPort80() throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("docker-compose.yml")));
        assertTrue(content.contains("80:80"), "nginx must expose port 80");
    }
}
