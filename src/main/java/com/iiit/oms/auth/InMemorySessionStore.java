package com.iiit.oms.auth;

import com.iiit.oms.model.UserSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store backed by ConcurrentHashMap. Default implementation
 * used when Redis is unavailable.
 */
public class InMemorySessionStore implements SessionStore {
    private final ConcurrentHashMap<String, UserSession> store = new ConcurrentHashMap<>();

    @Override
    public void put(String token, UserSession session) {
        store.put(token, session);
    }

    @Override
    public UserSession get(String token) {
        UserSession s = store.get(token);
        if (s != null && s.isExpired()) {
            store.remove(token);
            return null;
        }
        return s;
    }

    @Override
    public void remove(String token) {
        store.remove(token);
    }
}
