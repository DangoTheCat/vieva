package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.settings.RagSettings;
import com.example.vieva.domain.entities.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PgVectorAdapter implements DocumentChunkRepository {

    private final DocumentChunkJpaRepository jpaRepository;
    private final DocumentChunkPersistenceMapper mapper;
    private final EmbeddingModelPort embeddingModel;
    private final RagSettings settings;

    @Override
    public List<float[]> embedChunks(List<String> texts) {
        List<float[]> vectors = new java.util.ArrayList<>();
        int batchSize = Math.max(1,settings.getEmbeddingBatchSize());
        for (int start=0; start<texts.size(); start+=batchSize) {
            var batch=texts.subList(start,Math.min(start+batchSize,texts.size()));
            var result=embeddingModel.generateEmbeddings(batch);
            if (result == null || result.size()!=batch.size()) throw new AiServiceException("Embedding count mismatch");
            for (float[] vector: result) {
                if (vector==null || vector.length!=settings.getEmbeddingDimensions()) {
                    throw new AiServiceException("Embedding dimensions mismatch");
                }
            }
            vectors.addAll(result);
        }
        return vectors;
    }

    @Override
    public List<DocumentChunk> storeEmbeddings(List<DocumentChunk> chunks) {
        if (chunks.stream().anyMatch(c -> c.getEmbedding()==null)) {
            var vectors=embedChunks(chunks.stream().map(DocumentChunk::getContent).toList());
            List<DocumentChunk> embedded = new java.util.ArrayList<>();
            for(int i=0;i<chunks.size();i++) embedded.add(chunks.get(i).toBuilder().embedding(vectors.get(i)).build());
            chunks=embedded;
        }
        return saveAll(chunks);
    }

    @Override
    public List<ChunkSearchResult> similaritySearch(UUID subjectId, Collection<UUID> documentIds,
                                                   String query, double minSimilarity,int topK) {
        return searchSimilarChunks(subjectId,documentIds,embeddingModel.generateEmbedding(query),minSimilarity,topK);
    }

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

    @Override
    public void deleteByDocumentIds(Collection<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        jpaRepository.deleteByDocumentIdIn(documentIds);
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
