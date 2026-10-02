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
                .subjectId(entity.getSubject() != null ? entity.getSubject().getSubjectId() : null)
                .topicId(entity.getTopic() != null ? entity.getTopic().getTopicId() : null)
                .questionCode(entity.getQuestionCode())
                .status(entity.getStatus())
                .currentApprovedVersionId(entity.getCurrentApprovedVersionId())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public QuestionJpaEntity toEntity(Question domain) {
        if (domain == null) {
            return null;
        }
        SubjectJpaEntity subjectEntity = domain.getSubjectId() == null ? null
                : SubjectJpaEntity.builder().subjectId(domain.getSubjectId()).isNew(false).build();
        TopicJpaEntity topicEntity = domain.getTopicId() == null ? null
                : TopicJpaEntity.builder().topicId(domain.getTopicId()).isNew(false).build();

        return QuestionJpaEntity.builder()
                .questionId(domain.getQuestionId() != null ? domain.getQuestionId() : UUID.randomUUID())
                .subject(subjectEntity)
                .topic(topicEntity)
                .questionCode(domain.getQuestionCode())
                .status(domain.getStatus())
                .currentApprovedVersionId(domain.getCurrentApprovedVersionId())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                // Always merge: associations are id-only stubs that merge resolves against the session.
                .isNew(domain.getCreatedAt() == null)
                .build();
    }
}
