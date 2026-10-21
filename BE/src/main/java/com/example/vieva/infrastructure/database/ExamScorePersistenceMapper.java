package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.ExamScore;
import org.springframework.stereotype.Component;

@Component
public class ExamScorePersistenceMapper {

    public ExamScoreJpaEntity toEntity(ExamScore domain) {
        if (domain == null) {
            return null;
        }
        return ExamScoreJpaEntity.builder()
                .id(domain.getId())
                .attemptId(domain.getAttemptId())
                .totalScore(domain.getTotalScore())
                .grade(domain.getGrade())
                .detailedFeedback(domain.getDetailedFeedback())
                .scoringMode(domain.getScoringMode())
                .createdAt(domain.getCreatedAt())
                .isNewEntity(false)
                .build();
    }

    public ExamScore toDomain(ExamScoreJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ExamScore.builder()
                .id(entity.getId())
                .attemptId(entity.getAttemptId())
                .totalScore(entity.getTotalScore())
                .grade(entity.getGrade())
                .detailedFeedback(entity.getDetailedFeedback())
                .scoringMode(entity.getScoringMode())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
