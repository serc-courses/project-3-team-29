package com.iiit.oms.repository;

import com.iiit.oms.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findByUserID(String userID);
    List<User> findAll();
}
