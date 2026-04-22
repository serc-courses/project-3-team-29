package com.iiit.oms.auth;

import com.iiit.oms.model.UserSession;

/**
 * Abstraction for storing and retrieving user sessions by token.
 */
public interface SessionStore {
    void put(String token, UserSession session);
    UserSession get(String token);
    void remove(String token);
}
