package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.QuestionVersion;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class QuestionVersionPersistenceMapper {

    public QuestionVersion toDomain(QuestionVersionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return QuestionVersion.builder()
                .questionVersionId(entity.getQuestionVersionId())
                .questionId(entity.getQuestion() != null ? entity.getQuestion().getQuestionId() : null)
                .versionNumber(entity.getVersionNumber())
                .questionContent(entity.getQuestionContent())
                .referenceAnswer(entity.getReferenceAnswer())
                .bloomLevel(entity.getBloomLevel())
                .bloomConfirmed(entity.isBloomConfirmed())
                .generationMode(entity.getGenerationMode())
                .approvalStatus(entity.getApprovalStatus())
                .parentVersionId(entity.getParentVersionId())
                .generationRequestId(entity.getGenerationRequestId())
                .regenerationCount(entity.getRegenerationCount())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .rejectionReason(entity.getRejectionReason())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public QuestionVersionJpaEntity toEntity(QuestionVersion domain) {
        if (domain == null) {
            return null;
        }
        QuestionJpaEntity questionEntity = null;
        if (domain.getQuestionId() != null) {
            questionEntity = QuestionJpaEntity.builder()
                    .questionId(domain.getQuestionId())
                    .build();
        }

        return QuestionVersionJpaEntity.builder()
                .questionVersionId(domain.getQuestionVersionId() != null ? domain.getQuestionVersionId() : UUID.randomUUID())
                .question(questionEntity)
                .versionNumber(domain.getVersionNumber() != null ? domain.getVersionNumber() : 1)
                .questionContent(domain.getQuestionContent())
                .referenceAnswer(domain.getReferenceAnswer())
                .bloomLevel(domain.getBloomLevel())
                .bloomConfirmed(domain.isBloomConfirmed())
                .generationMode(domain.getGenerationMode())
                .approvalStatus(domain.getApprovalStatus())
                .parentVersionId(domain.getParentVersionId())
                .generationRequestId(domain.getGenerationRequestId())
                .regenerationCount(domain.getRegenerationCount())
                .reviewedBy(domain.getReviewedBy())
                .reviewedAt(domain.getReviewedAt())
                .rejectionReason(domain.getRejectionReason())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                // Carrying the lock token makes merge() reject stale writes (BR-08).
                .version(domain.getVersion())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }
}
