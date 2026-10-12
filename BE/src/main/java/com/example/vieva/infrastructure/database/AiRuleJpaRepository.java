package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiRuleJpaRepository extends JpaRepository<AiRuleJpaEntity, UUID> {
}
