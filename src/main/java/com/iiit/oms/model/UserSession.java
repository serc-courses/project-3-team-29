package com.iiit.oms.model;

public class UserSession {
    private final String token;
    private final User user;
    private final long createdAt;

    private static final long TTL_MS = 24L * 60 * 60 * 1000; // 24 hours

    public UserSession(String token, User user) {
        this.token = token;
        this.user = user;
        this.createdAt = System.currentTimeMillis();
    }

    public String getToken()  { return token; }
    public User getUser()     { return user; }
    public long getCreatedAt(){ return createdAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > TTL_MS;
    }
}
