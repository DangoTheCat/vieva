package com.example.vieva.domain.repository;

import com.example.vieva.domain.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserCode(String userCode);
    boolean existsByEmail(String email);
    boolean existsByUserCode(String userCode);
    User save(User user);
    void deleteById(UUID userId);
}
