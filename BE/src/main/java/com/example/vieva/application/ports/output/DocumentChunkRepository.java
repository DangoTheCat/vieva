package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.DocumentChunk;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentChunkRepository {
    DocumentChunk save(DocumentChunk chunk);
    List<DocumentChunk> saveAll(List<DocumentChunk> chunks);
    Optional<DocumentChunk> findById(UUID chunkId);
    List<DocumentChunk> findAllByIds(Collection<UUID> chunkIds);
    List<DocumentChunk> findByDocumentId(UUID documentId);
    List<ChunkSearchResult> searchSimilarChunks(UUID subjectId, float[] queryEmbedding, double maxDistance, int limit);
    void deleteByDocumentId(UUID documentId);
}
