package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentChunkPersistenceMapper {

    public DocumentChunk toDomain(DocumentChunkJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return DocumentChunk.builder()
                .chunkId(entity.getChunkId())
                .documentId(entity.getDocument() != null ? entity.getDocument().getDocumentId() : null)
                .chunkIndex(entity.getChunkIndex())
                .content(entity.getContent())
                .tokenCount(entity.getTokenCount())
                .vectorId(entity.getVectorId())
                .embedding(entity.getEmbedding())
                .metadataJson(entity.getMetadataJson())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public DocumentChunkJpaEntity toEntity(DocumentChunk domain) {
        if (domain == null) {
            return null;
        }
        CourseDocumentJpaEntity docEntity = null;
        if (domain.getDocumentId() != null) {
            docEntity = CourseDocumentJpaEntity.builder()
                    .documentId(domain.getDocumentId())
                    .build();
        }

        return DocumentChunkJpaEntity.builder()
                .chunkId(domain.getChunkId() != null ? domain.getChunkId() : UUID.randomUUID())
                .document(docEntity)
                .chunkIndex(domain.getChunkIndex())
                .content(domain.getContent())
                .tokenCount(domain.getTokenCount())
                .vectorId(domain.getVectorId())
                .embedding(domain.getEmbedding())
                .metadataJson(domain.getMetadataJson())
                .createdAt(domain.getCreatedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }

    public static String formatVector(float[] vector) {
        if (vector == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
