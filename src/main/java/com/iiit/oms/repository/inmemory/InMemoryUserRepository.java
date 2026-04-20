package com.iiit.oms.repository.inmemory;

import com.iiit.oms.model.User;
import com.iiit.oms.repository.UserRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> byId       = new ConcurrentHashMap<>();
    private final Map<String, User> byUsername = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        byId.put(user.getUserID(), user);
        byUsername.put(user.getUsername().toLowerCase(), user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(byUsername.get(username.toLowerCase()));
    }

    @Override
    public Optional<User> findByUserID(String userID) {
        return Optional.ofNullable(byId.get(userID));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(byId.values());
    }
}
