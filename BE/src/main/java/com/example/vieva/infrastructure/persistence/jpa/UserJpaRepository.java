package com.example.vieva.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findByEmail(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findByUserCode(String userCode);

    boolean existsByEmail(String email);
    boolean existsByUserCode(String userCode);
}
