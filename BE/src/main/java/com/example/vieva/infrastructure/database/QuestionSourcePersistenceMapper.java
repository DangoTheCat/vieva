package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.QuestionSource;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class QuestionSourcePersistenceMapper {

    public QuestionSource toDomain(QuestionSourceJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return QuestionSource.builder()
                .questionSourceId(entity.getQuestionSourceId())
                .questionVersionId(entity.getQuestionVersion() != null ? entity.getQuestionVersion().getQuestionVersionId() : null)
                .chunkId(entity.getChunk() != null ? entity.getChunk().getChunkId() : null)
                .documentId(entity.getDocumentId())
                .sourceOrder(entity.getSourceOrder())
                .documentName(entity.getDocumentName())
                .citationQuote(entity.getCitationQuote())
                .similarityScore(entity.getSimilarityScore())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public QuestionSourceJpaEntity toEntity(QuestionSource domain) {
        if (domain == null) {
            return null;
        }
        QuestionVersionJpaEntity versionEntity = null;
        if (domain.getQuestionVersionId() != null) {
            versionEntity = QuestionVersionJpaEntity.builder()
                    .questionVersionId(domain.getQuestionVersionId())
                    .build();
        }

        DocumentChunkJpaEntity chunkEntity = null;
        if (domain.getChunkId() != null) {
            chunkEntity = DocumentChunkJpaEntity.builder()
                    .chunkId(domain.getChunkId())
                    .build();
        }

        return QuestionSourceJpaEntity.builder()
                .questionSourceId(domain.getQuestionSourceId() != null ? domain.getQuestionSourceId() : UUID.randomUUID())
                .questionVersion(versionEntity)
                .chunk(chunkEntity)
                .documentId(domain.getDocumentId())
                .sourceOrder(domain.getSourceOrder() != null ? domain.getSourceOrder() : 1)
                .documentName(domain.getDocumentName())
                .citationQuote(domain.getCitationQuote())
                .similarityScore(domain.getSimilarityScore())
                .createdAt(domain.getCreatedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }
}
