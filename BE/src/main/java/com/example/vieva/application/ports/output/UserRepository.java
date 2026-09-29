package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.User;

import com.example.vieva.application.ports.input.UserSearchCriteria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserCode(String userCode);
    List<User> findAll();
    PagedResult<User> findAll(UserSearchCriteria criteria);
    long countActiveAdmins();
    boolean existsByEmail(String email);
    boolean existsByUserCode(String userCode);
    boolean existsByEmailAndUserIdNot(String email, UUID userId);
    boolean existsByUserCodeAndUserIdNot(String userCode, UUID userId);
    User save(User user);
    void deleteById(UUID userId);
}
