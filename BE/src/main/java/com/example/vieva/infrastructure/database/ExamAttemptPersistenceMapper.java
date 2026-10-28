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
                .examId(domain.getExamId())
                .status(domain.getStatus())
                .currentQuestionIndex(domain.getCurrentQuestionIndex())
                .totalQuestions(domain.getTotalQuestions())
                .totalScore(domain.getTotalScore())
                .gradeLevel(domain.getGradeLevel())
                .startedAt(domain.getStartedAt())
                .completedAt(domain.getCompletedAt())
                .pausedAt(domain.getPausedAt())
                .resumedAt(domain.getResumedAt())
                .remainingSeconds(domain.getRemainingSeconds())
                .graceDeadline(domain.getGraceDeadline())
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
                .examId(entity.getExamId())
                .status(entity.getStatus())
                .currentQuestionIndex(entity.getCurrentQuestionIndex())
                .totalQuestions(entity.getTotalQuestions())
                .totalScore(entity.getTotalScore())
                .gradeLevel(entity.getGradeLevel())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .pausedAt(entity.getPausedAt())
                .resumedAt(entity.getResumedAt())
                .remainingSeconds(entity.getRemainingSeconds())
                .graceDeadline(entity.getGraceDeadline())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
