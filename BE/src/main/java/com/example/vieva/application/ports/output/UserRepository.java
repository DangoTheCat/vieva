package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.User;

import com.example.vieva.application.ports.input.UserSearchCriteria;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserCode(String userCode);
    PagedResult<User> findAll(UserSearchCriteria criteria);
    long countActiveAdminsForUpdate();
    boolean existsByEmail(String email);
    boolean existsByUserCode(String userCode);
    User save(User user);
}
