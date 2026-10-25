package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.ExamAttemptQuestionSnapshot;
import org.springframework.stereotype.Component;

@Component
public class ExamAttemptQuestionSnapshotPersistenceMapper {

    public ExamAttemptQuestionSnapshotJpaEntity toEntity(ExamAttemptQuestionSnapshot domain) {
        if (domain == null) {
            return null;
        }
        return ExamAttemptQuestionSnapshotJpaEntity.builder()
                .id(domain.getId())
                .attemptId(domain.getAttemptId())
                .questionId(domain.getQuestionId())
                .questionVersionId(domain.getQuestionVersionId())
                .orderIndex(domain.getOrderIndex())
                .questionText(domain.getQuestionText())
                .expectedAnswer(domain.getExpectedAnswer())
                .rubricSnapshot(domain.getRubricSnapshot())
                .createdAt(domain.getCreatedAt())
                .isNewEntity(false)
                .build();
    }

    public ExamAttemptQuestionSnapshot toDomain(ExamAttemptQuestionSnapshotJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ExamAttemptQuestionSnapshot.builder()
                .id(entity.getId())
                .attemptId(entity.getAttemptId())
                .questionId(entity.getQuestionId())
                .questionVersionId(entity.getQuestionVersionId())
                .orderIndex(entity.getOrderIndex())
                .questionText(entity.getQuestionText())
                .expectedAnswer(entity.getExpectedAnswer())
                .rubricSnapshot(entity.getRubricSnapshot())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
