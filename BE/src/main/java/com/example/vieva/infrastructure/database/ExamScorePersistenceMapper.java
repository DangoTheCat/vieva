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
                .aiScore(domain.getAiScore())
                .aiFeedback(domain.getAiFeedback())
                .teacherScore(domain.getTeacherScore())
                .teacherFeedback(domain.getTeacherFeedback())
                .teacherId(domain.getTeacherId())
                .overrideTotalScore(domain.getOverrideTotalScore())
                .overrideBy(domain.getOverrideBy())
                .overrideAt(domain.getOverrideAt())
                .overrideReason(domain.getOverrideReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
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
                .aiScore(entity.getAiScore())
                .aiFeedback(entity.getAiFeedback())
                .teacherScore(entity.getTeacherScore())
                .teacherFeedback(entity.getTeacherFeedback())
                .teacherId(entity.getTeacherId())
                .overrideTotalScore(entity.getOverrideTotalScore())
                .overrideBy(entity.getOverrideBy())
                .overrideAt(entity.getOverrideAt())
                .overrideReason(entity.getOverrideReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
