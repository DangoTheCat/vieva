package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.Question;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class QuestionPersistenceMapper {

    public Question toDomain(QuestionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Question.builder()
                .questionId(entity.getQuestionId())
                .topicId(entity.getTopic() != null ? entity.getTopic().getTopicId() : null)
                .questionCode(entity.getQuestionCode())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public QuestionJpaEntity toEntity(Question domain) {
        if (domain == null) {
            return null;
        }
        TopicJpaEntity topicEntity = null;
        if (domain.getTopicId() != null) {
            topicEntity = TopicJpaEntity.builder()
                    .topicId(domain.getTopicId())
                    .build();
        }

        return QuestionJpaEntity.builder()
                .questionId(domain.getQuestionId() != null ? domain.getQuestionId() : UUID.randomUUID())
                .topic(topicEntity)
                .questionCode(domain.getQuestionCode())
                .status(domain.getStatus())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }
}
