package com.iiit.oms.auth;

public interface RevocationStore {
    void revoke(String jti, long ttlSeconds);
    boolean isRevoked(String jti);
}
