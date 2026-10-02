package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.domain.entities.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DocumentChunkRepositoryImpl implements DocumentChunkRepository {

    private final DocumentChunkJpaRepository jpaRepository;
    private final DocumentChunkPersistenceMapper mapper;

    @Override
    public DocumentChunk save(DocumentChunk chunk) {
        DocumentChunkJpaEntity entity = mapper.toEntity(chunk);
        DocumentChunkJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<DocumentChunk> saveAll(List<DocumentChunk> chunks) {
        List<DocumentChunkJpaEntity> entities = chunks.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DocumentChunk> findById(UUID chunkId) {
        return jpaRepository.findById(chunkId)
                .map(mapper::toDomain);
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
    public List<ChunkSearchResult> searchSimilarChunks(UUID subjectId, float[] queryEmbedding, double maxDistance, int limit) {
        String vectorLiteral = DocumentChunkPersistenceMapper.formatVector(queryEmbedding);
        List<ChunkSearchResultProjection> projections = jpaRepository.searchSimilarChunks(
                subjectId, vectorLiteral, maxDistance, limit);

        return projections.stream()
                .map(p -> ChunkSearchResult.of(
                        p.getChunkId(),
                        p.getDocumentId(),
                        p.getDocumentName(),
                        p.getChunkIndex(),
                        p.getContent(),
                        p.getDistance() != null ? p.getDistance() : 1.0))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        jpaRepository.deleteByDocumentId(documentId);
    }
}
