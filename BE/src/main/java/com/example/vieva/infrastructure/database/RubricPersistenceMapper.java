package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.PerformanceLevel;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RubricPersistenceMapper {

    private static final TypeReference<List<PerformanceLevel>> LEVELS_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public Rubric toDomain(RubricJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Rubric.builder()
                .rubricId(entity.getRubricId())
                .questionVersionId(entity.getQuestionVersion() != null ? entity.getQuestionVersion().getQuestionVersionId() : null)
                .rubricName(entity.getRubricName())
                .totalPoints(entity.getTotalPoints())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public RubricJpaEntity toEntity(Rubric domain) {
        if (domain == null) {
            return null;
        }
        QuestionVersionJpaEntity versionEntity = null;
        if (domain.getQuestionVersionId() != null) {
            versionEntity = QuestionVersionJpaEntity.builder()
                    .questionVersionId(domain.getQuestionVersionId())
                    .build();
        }

        return RubricJpaEntity.builder()
                .rubricId(domain.getRubricId() != null ? domain.getRubricId() : UUID.randomUUID())
                .questionVersion(versionEntity)
                .rubricName(domain.getRubricName())
                .totalPoints(domain.getTotalPoints())
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }

    public RubricCriterion toDomain(RubricCriterionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return RubricCriterion.builder()
                .criterionId(entity.getCriterionId())
                .rubricId(entity.getRubric() != null ? entity.getRubric().getRubricId() : null)
                .criterionName(entity.getCriterionName())
                .maxPoints(entity.getMaxPoints())
                .achievementDescriptors(entity.getAchievementDescriptors())
                .performanceLevels(readLevels(entity.getPerformanceLevels()))
                .orderIndex(entity.getOrderIndex())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public RubricCriterionJpaEntity toEntity(RubricCriterion domain) {
        if (domain == null) {
            return null;
        }
        RubricJpaEntity rubricEntity = null;
        if (domain.getRubricId() != null) {
            rubricEntity = RubricJpaEntity.builder()
                    .rubricId(domain.getRubricId())
                    .build();
        }

        return RubricCriterionJpaEntity.builder()
                .criterionId(domain.getCriterionId() != null ? domain.getCriterionId() : UUID.randomUUID())
                .rubric(rubricEntity)
                .criterionName(domain.getCriterionName())
                .maxPoints(domain.getMaxPoints())
                .achievementDescriptors(domain.getAchievementDescriptors())
                .performanceLevels(writeLevels(domain.getPerformanceLevels()))
                .orderIndex(domain.getOrderIndex() != null ? domain.getOrderIndex() : 1)
                .createdAt(domain.getCreatedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }

    private List<PerformanceLevel> readLevels(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(json, LEVELS_TYPE));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted performance_levels JSON on rubric criterion", e);
        }
    }

    private String writeLevels(List<PerformanceLevel> levels) {
        if (levels == null || levels.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(levels);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize performance levels", e);
        }
    }
}
