package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.AiRuleRepository;
import com.example.vieva.domain.entities.AiRule;
import com.example.vieva.domain.entities.AiRuleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiRuleRepositoryImpl implements AiRuleRepository {

    private final AiRuleJpaRepository jpaRepository;
    private final AiRulePersistenceMapper mapper;

    @Override
    public Optional<AiRule> findActiveByCode(String ruleCode) {
        return jpaRepository.findByRuleCodeAndActiveTrue(ruleCode)
                .map(mapper::toDomain);
    }

    @Override
    public List<AiRule> findActiveByType(AiRuleType ruleType) {
        return jpaRepository.findByRuleTypeAndActiveTrueOrderByRuleCodeAsc(ruleType).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
