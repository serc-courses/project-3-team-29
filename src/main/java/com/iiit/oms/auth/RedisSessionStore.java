package com.iiit.oms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiit.oms.model.User;
import com.iiit.oms.model.UserSession;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Redis-backed session store for user tokens.
 * Keys: oms:session:{token} → JSON user payload, TTL 86400s (24h).
 * Falls back to an in-memory ConcurrentHashMap if Redis is unavailable.
 */
public class RedisSessionStore implements SessionStore {
    private static final Logger LOGGER = Logger.getLogger(RedisSessionStore.class.getName());
    private static final String KEY_PREFIX = "oms:session:";
    private static final int TTL_SECONDS = 86400; // 24h

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, UserSession> fallback = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable;

    public RedisSessionStore(String host, int port) {
        JedisPool pool = null;
        boolean available = false;
        try {
            JedisPoolConfig cfg = new JedisPoolConfig();
            cfg.setMaxTotal(8);
            cfg.setMaxIdle(2);
            pool = new JedisPool(cfg, host, port, 1000);
            try (Jedis j = pool.getResource()) {
                j.ping();
                available = true;
                LOGGER.info("RedisSessionStore connected at " + host + ":" + port);
            }
        } catch (Exception ex) {
            LOGGER.warning("RedisSessionStore: Redis unavailable, using in-memory fallback: " + ex.getMessage());
            if (pool != null) { try { pool.close(); } catch (Exception ignored) {} pool = null; }
        }
        this.jedisPool = pool;
        this.redisAvailable = available;
    }

    @Override
    public void put(String token, UserSession session) {
        if (redisAvailable && jedisPool != null) {
            try (Jedis j = jedisPool.getResource()) {
                String json = toJson(session.getUser());
                j.setex(KEY_PREFIX + token, TTL_SECONDS, json);
                return;
            } catch (Exception ex) {
                LOGGER.warning("Redis session put failed, using fallback: " + ex.getMessage());
                redisAvailable = false;
            }
        }
        fallback.put(token, session);
    }

    @Override
    public UserSession get(String token) {
        if (redisAvailable && jedisPool != null) {
            try (Jedis j = jedisPool.getResource()) {
                String json = j.get(KEY_PREFIX + token);
                if (json == null) return null;
                // Refresh TTL on access
                j.expire(KEY_PREFIX + token, TTL_SECONDS);
                User user = objectMapper.readValue(json, User.class);
                return new UserSession(token, user);
            } catch (Exception ex) {
                LOGGER.warning("Redis session get failed, using fallback: " + ex.getMessage());
                redisAvailable = false;
            }
        }
        UserSession s = fallback.get(token);
        if (s != null && s.isExpired()) {
            fallback.remove(token);
            return null;
        }
        return s;
    }

    @Override
    public void remove(String token) {
        if (redisAvailable && jedisPool != null) {
            try (Jedis j = jedisPool.getResource()) {
                j.del(KEY_PREFIX + token);
                return;
            } catch (Exception ex) {
                LOGGER.warning("Redis session remove failed, using fallback: " + ex.getMessage());
                redisAvailable = false;
            }
        }
        fallback.remove(token);
    }

    private String toJson(User user) {
        try {
            return objectMapper.writeValueAsString(user);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize user session", ex);
        }
    }
}
