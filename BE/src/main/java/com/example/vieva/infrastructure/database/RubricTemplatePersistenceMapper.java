package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.RubricTemplate;
import org.springframework.stereotype.Component;

@Component
public class RubricTemplatePersistenceMapper {

    public RubricTemplate toDomain(RubricTemplateJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return RubricTemplate.builder()
                .id(entity.getId())
                .subjectId(entity.getSubjectId())
                .name(entity.getName())
                .description(entity.getDescription())
                .criteriaJson(entity.getCriteriaJson())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public RubricTemplateJpaEntity toEntity(RubricTemplate domain) {
        if (domain == null) {
            return null;
        }
        return RubricTemplateJpaEntity.builder()
                .id(domain.getId())
                .subjectId(domain.getSubjectId())
                .name(domain.getName())
                .description(domain.getDescription())
                .criteriaJson(domain.getCriteriaJson())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
