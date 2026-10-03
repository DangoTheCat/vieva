package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.AiRule;
import org.springframework.stereotype.Component;

@Component
public class AiRulePersistenceMapper {

    public AiRule toDomain(AiRuleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AiRule.builder()
                .ruleId(entity.getRuleId())
                .ruleCode(entity.getRuleCode())
                .ruleName(entity.getRuleName())
                .ruleType(entity.getRuleType())
                .promptContent(entity.getPromptContent())
                .temperature(entity.getTemperature())
                .maxTokens(entity.getMaxTokens())
                .active(entity.getActive())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
