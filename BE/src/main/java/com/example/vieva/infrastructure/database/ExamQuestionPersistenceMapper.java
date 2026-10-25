package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.ExamQuestion;
import org.springframework.stereotype.Component;

@Component
public class ExamQuestionPersistenceMapper {

    public ExamQuestionJpaEntity toEntity(ExamQuestion domain) {
        if (domain == null) {
            return null;
        }
        return ExamQuestionJpaEntity.builder()
                .id(domain.getId())
                .examId(domain.getExamId())
                .questionId(domain.getQuestionId())
                .questionVersionId(domain.getQuestionVersionId())
                .orderIndex(domain.getOrderIndex())
                .createdAt(domain.getCreatedAt())
                .isNewEntity(false)
                .build();
    }

    public ExamQuestion toDomain(ExamQuestionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ExamQuestion.builder()
                .id(entity.getId())
                .examId(entity.getExamId())
                .questionId(entity.getQuestionId())
                .questionVersionId(entity.getQuestionVersionId())
                .orderIndex(entity.getOrderIndex())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
