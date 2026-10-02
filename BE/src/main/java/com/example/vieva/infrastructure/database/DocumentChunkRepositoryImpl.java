package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.domain.entities.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DocumentChunkRepositoryImpl implements DocumentChunkRepository {

    private final DocumentChunkJpaRepository jpaRepository;
    private final DocumentChunkPersistenceMapper mapper;

    @Override
    public List<DocumentChunk> saveAll(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<DocumentChunkJpaEntity> entities = chunks.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentChunk> findAllByIds(Collection<UUID> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(chunkIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentChunk> findByDocumentId(UUID documentId) {
        return jpaRepository.findByDocument_DocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChunkSearchResult> searchSimilarChunks(UUID subjectId, Collection<UUID> documentIds,
                                                       float[] queryEmbedding, double minSimilarity, int limit) {
        if (documentIds == null || documentIds.isEmpty() || queryEmbedding == null || limit <= 0) {
            return List.of();
        }
        String vectorLiteral = DocumentChunkPersistenceMapper.formatVector(queryEmbedding);
        // cosine distance = 1 - cosine similarity
        double maxDistance = 1.0 - minSimilarity;
        return jpaRepository.searchSimilarChunks(subjectId, documentIds, vectorLiteral, maxDistance, limit).stream()
                .map(p -> ChunkSearchResult.of(
                        p.getChunkId(),
                        p.getDocumentId(),
                        p.getDocumentName(),
                        p.getChunkIndex(),
                        parsePage(p.getPageStart()),
                        p.getContent(),
                        p.getDistance() != null ? p.getDistance() : 1.0))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        jpaRepository.deleteByDocumentId(documentId);
    }

    private static Integer parsePage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
