package com.iiit.oms.processor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.logging.Logger;

/**
 * Thin Redis-backed distributed lock for scheduler dedup across replicas.
 *
 * Uses SET NX EX to atomically acquire the lock. Only one replica executes
 * the guarded work per interval; others skip silently.
 *
 * If Redis is unavailable, the lock degrades gracefully: tryAcquire() returns
 * true so a single-instance deployment continues working without Redis.
 */
public class DistributedLock {

    private static final Logger LOGGER = Logger.getLogger(DistributedLock.class.getName());

    private final JedisPool jedisPool;
    private final String key;
    private final String instanceId;
    private final long ttlSeconds;

    public DistributedLock(JedisPool jedisPool, String key, String instanceId, long ttlSeconds) {
        this.jedisPool = jedisPool;
        this.key = key;
        this.instanceId = instanceId;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Attempts to acquire the lock.
     * @return true if acquired (this replica should run), false if another replica holds it.
     */
    public boolean tryAcquire() {
        if (jedisPool == null) return true; // no Redis → single-instance fallback
        try (Jedis j = jedisPool.getResource()) {
            String result = j.set(key, instanceId, SetParams.setParams().nx().ex(ttlSeconds));
            return "OK".equals(result);
        } catch (Exception ex) {
            LOGGER.warning("DistributedLock: Redis unavailable for key=" + key +
                    " — proceeding without lock: " + ex.getMessage());
            return true; // fail-open: don't block the scheduler
        }
    }
}
