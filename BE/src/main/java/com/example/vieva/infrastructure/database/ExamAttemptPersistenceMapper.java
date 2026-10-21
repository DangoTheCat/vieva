package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.ExamAttempt;
import org.springframework.stereotype.Component;

@Component
public class ExamAttemptPersistenceMapper {

    public ExamAttemptJpaEntity toEntity(ExamAttempt domain) {
        if (domain == null) {
            return null;
        }
        return ExamAttemptJpaEntity.builder()
                .id(domain.getId())
                .studentId(domain.getStudentId())
                .subjectId(domain.getSubjectId())
                .status(domain.getStatus())
                .currentQuestionIndex(domain.getCurrentQuestionIndex())
                .totalQuestions(domain.getTotalQuestions())
                .totalScore(domain.getTotalScore())
                .gradeLevel(domain.getGradeLevel())
                .startedAt(domain.getStartedAt())
                .completedAt(domain.getCompletedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .isNewEntity(false)
                .build();
    }

    public ExamAttempt toDomain(ExamAttemptJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ExamAttempt.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .subjectId(entity.getSubjectId())
                .status(entity.getStatus())
                .currentQuestionIndex(entity.getCurrentQuestionIndex())
                .totalQuestions(entity.getTotalQuestions())
                .totalScore(entity.getTotalScore())
                .gradeLevel(entity.getGradeLevel())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
