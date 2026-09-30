package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Integer> {
    Optional<RoleJpaEntity> findByRoleCode(String roleCode);
}
