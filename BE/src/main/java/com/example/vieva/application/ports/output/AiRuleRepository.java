package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.AiRule;
import com.example.vieva.domain.entities.AiRuleType;

import java.util.List;
import java.util.Optional;

public interface AiRuleRepository {
    Optional<AiRule> findActiveByCode(String ruleCode);
    List<AiRule> findActiveByType(AiRuleType ruleType);
}
