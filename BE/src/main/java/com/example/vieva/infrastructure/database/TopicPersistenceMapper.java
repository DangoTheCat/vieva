package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.Topic;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TopicPersistenceMapper {

    public Topic toDomain(TopicJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Topic.builder()
                .topicId(entity.getTopicId())
                .subjectId(entity.getSubject() != null ? entity.getSubject().getSubjectId() : null)
                .topicName(entity.getTopicName())
                .orderIndex(entity.getOrderIndex())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public TopicJpaEntity toEntity(Topic domain) {
        if (domain == null) {
            return null;
        }
        SubjectJpaEntity subjectEntity = null;
        if (domain.getSubjectId() != null) {
            subjectEntity = SubjectJpaEntity.builder()
                    .subjectId(domain.getSubjectId())
                    .build();
        }

        return TopicJpaEntity.builder()
                .topicId(domain.getTopicId() != null ? domain.getTopicId() : UUID.randomUUID())
                .subject(subjectEntity)
                .topicName(domain.getTopicName())
                .orderIndex(domain.getOrderIndex() != null ? domain.getOrderIndex() : 1)
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }
}
