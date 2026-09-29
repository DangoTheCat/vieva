package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID>, JpaSpecificationExecutor<UserJpaEntity> {

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findByEmailAndDeletedAtIsNull(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findByUserCodeAndDeletedAtIsNull(String userCode);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findByEmail(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findByUserCode(String userCode);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.deletedAt IS NULL")
    List<UserJpaEntity> findAllActive();

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByUserCodeAndDeletedAtIsNull(String userCode);

    boolean existsByEmailAndUserIdNotAndDeletedAtIsNull(String email, UUID userId);

    boolean existsByUserCodeAndUserIdNotAndDeletedAtIsNull(String userCode, UUID userId);

    @Query("SELECT COUNT(DISTINCT u) FROM UserJpaEntity u JOIN u.userRoles ur JOIN ur.role r " +
           "WHERE (r.roleCode = 'ROLE_ADMIN' OR r.roleCode = 'ADMIN') " +
           "AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    long countActiveAdmins();
}
