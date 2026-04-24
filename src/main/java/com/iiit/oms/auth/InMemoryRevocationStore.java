package com.iiit.oms.auth;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRevocationStore implements RevocationStore {

    // jti → expiry wall-clock time in ms
    private final ConcurrentHashMap<String, Long> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, long ttlSeconds) {
        if (jti == null || ttlSeconds <= 0) return;
        revoked.put(jti, System.currentTimeMillis() + ttlSeconds * 1000);
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Long expiry = revoked.get(jti);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }
}
