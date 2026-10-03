package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.AiRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiRuleJpaRepository extends JpaRepository<AiRuleJpaEntity, UUID> {
    Optional<AiRuleJpaEntity> findByRuleCodeAndActiveTrue(String ruleCode);
    List<AiRuleJpaEntity> findByRuleTypeAndActiveTrueOrderByRuleCodeAsc(AiRuleType ruleType);
}
