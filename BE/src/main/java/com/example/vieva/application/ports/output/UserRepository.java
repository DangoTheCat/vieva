package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.User;

import com.example.vieva.application.ports.input.UserSearchCriteria;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID userId);
    List<User> findAllByIds(Collection<UUID> userIds);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserCode(String userCode);
    PagedResult<User> findAll(UserSearchCriteria criteria);
    long countActiveAdmins();
    boolean existsByEmail(String email);
    boolean existsByUserCode(String userCode);
    User save(User user);
    /**
     * Forces SQL execution inside the call, so unique-index violations surface here
     * instead of at transaction commit (concurrent-registration handling).
     */
    User saveAndFlush(User user);
}
