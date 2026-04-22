package com.iiit.oms.idempotency;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import java.util.logging.Logger;

/**
 * Redis-backed idempotency store using SETNX (SET if Not eXists) semantics.
 * Falls back to in-memory store if Redis is unavailable.
 */
public class RedisIdempotencyStore implements IdempotencyStore {
    private static final Logger LOGGER = Logger.getLogger(RedisIdempotencyStore.class.getName());
    private static final String KEY_PREFIX = "oms:idem:";
    private static final String DEDUP_HITS_KEY = "oms:dedup:hits";

    private final JedisPool jedisPool;
    private final IdempotencyStore fallback;
    private volatile boolean redisAvailable;

    public RedisIdempotencyStore(String host, int port) {
        this.fallback = new InMemoryIdempotencyStore();
        JedisPool pool = null;
        boolean available = false;
        try {
            JedisPoolConfig cfg = new JedisPoolConfig();
            cfg.setMaxTotal(8);
            cfg.setMaxIdle(2);
            pool = new JedisPool(cfg, host, port, 1000); // 1s connection timeout
            // Test connection
            try (Jedis j = pool.getResource()) {
                j.ping();
                available = true;
                LOGGER.info("Redis idempotency store connected at " + host + ":" + port);
            }
        } catch (Exception ex) {
            LOGGER.warning("Redis not available at " + host + ":" + port + " – using in-memory fallback: " + ex.getMessage());
            if (pool != null) {
                try { pool.close(); } catch (Exception ignored) { }
            }
            pool = null;
        }
        this.jedisPool = pool;
        this.redisAvailable = available;
    }

    @Override
    public boolean registerIfAbsent(String key, String orderID, int ttlSeconds) {
        if (!redisAvailable || jedisPool == null) {
            return fallback.registerIfAbsent(key, orderID, ttlSeconds);
        }
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(KEY_PREFIX + key, orderID,
                    SetParams.setParams().nx().ex((long) ttlSeconds));
            return "OK".equals(result); // OK = newly set, null = already existed
        } catch (Exception ex) {
            LOGGER.warning("Redis error, falling back to in-memory: " + ex.getMessage());
            redisAvailable = false;
            return fallback.registerIfAbsent(key, orderID, ttlSeconds);
        }
    }

    @Override
    public String getOrderId(String key) {
        if (!redisAvailable || jedisPool == null) {
            return fallback.getOrderId(key);
        }
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(KEY_PREFIX + key);
        } catch (Exception ex) {
            LOGGER.warning("Redis error, falling back to in-memory: " + ex.getMessage());
            redisAvailable = false;
            return fallback.getOrderId(key);
        }
    }

    @Override
    public String getStatus() {
        return redisAvailable ? "REDIS (connected, dedupHits=" + getDedupHits() + ")" : "IN_MEMORY (Redis unavailable, fallback)";
    }

    @Override
    public void incrementDedupHits() {
        if (!redisAvailable || jedisPool == null) {
            fallback.incrementDedupHits();
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.incr(DEDUP_HITS_KEY);
        } catch (Exception ex) {
            LOGGER.warning("Redis error incrementing dedup hits: " + ex.getMessage());
            fallback.incrementDedupHits();
        }
    }

    @Override
    public long getDedupHits() {
        if (!redisAvailable || jedisPool == null) {
            return fallback.getDedupHits();
        }
        try (Jedis jedis = jedisPool.getResource()) {
            String val = jedis.get(DEDUP_HITS_KEY);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception ex) {
            LOGGER.warning("Redis error getting dedup hits: " + ex.getMessage());
            return fallback.getDedupHits();
        }
    }

    /**
     * Flush all OMS idempotency keys from Redis.
     * Called on clean-start to keep the idempotency store in sync with the DB.
     */
    public void flushAll() {
        if (!redisAvailable || jedisPool == null) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            // Scan and delete only our namespaced keys, not all Redis data
            String cursor = "0";
            do {
                redis.clients.jedis.resps.ScanResult<String> result =
                        jedis.scan(cursor, new redis.clients.jedis.params.ScanParams().match(KEY_PREFIX + "*").count(200));
                cursor = result.getCursor();
                if (!result.getResult().isEmpty()) {
                    jedis.del(result.getResult().toArray(new String[0]));
                }
            } while (!"0".equals(cursor));
            // Also reset dedup counter
            jedis.del(DEDUP_HITS_KEY);
            LOGGER.info("Flushed OMS idempotency keys from Redis");
        } catch (Exception ex) {
            LOGGER.warning("Could not flush Redis idempotency keys: " + ex.getMessage());
        }
    }
}

