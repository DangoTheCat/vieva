package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserCode(String userCode);
    List<User> findAll();
    boolean existsByEmail(String email);
    boolean existsByUserCode(String userCode);
    User save(User user);
    void deleteById(UUID userId);
}
