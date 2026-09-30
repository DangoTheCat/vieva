package com.example.vieva.infrastructure.database;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID>, JpaSpecificationExecutor<UserJpaEntity> {

    // findById is inherited — @SQLRestriction filters deleted users automatically
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findByEmail(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserJpaEntity> findByUserCode(String userCode);

    boolean existsByEmail(String email);

    boolean existsByUserCode(String userCode);

    /**
     * Locks active admin user rows with PESSIMISTIC_WRITE (SELECT ... FOR UPDATE).
     * Selecting entities instead of COUNT(DISTINCT ...) avoids the PostgreSQL runtime error:
     * "FOR UPDATE is not allowed with aggregate functions".
     *
     * Must be called inside an active @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserJpaEntity u JOIN u.userRoles ur JOIN ur.role r " +
           "WHERE (r.roleCode = 'ROLE_ADMIN' OR r.roleCode = 'ADMIN') " +
           "AND u.status = 'ACTIVE'")
    List<UserJpaEntity> findActiveAdminsForUpdate();
}
