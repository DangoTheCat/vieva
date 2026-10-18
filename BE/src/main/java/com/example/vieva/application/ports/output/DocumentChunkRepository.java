package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.DocumentChunk;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository {
    List<float[]> embedChunks(List<String> texts);
    List<DocumentChunk> storeEmbeddings(List<DocumentChunk> chunks);
    List<ChunkSearchResult> similaritySearch(UUID subjectId, Collection<UUID> documentIds,
                                           String topicKeywords, double minSimilarity, int topK);
    List<DocumentChunk> saveAll(List<DocumentChunk> chunks);
    List<DocumentChunk> findAllByIds(Collection<UUID> chunkIds);
    List<DocumentChunk> findByDocumentId(UUID documentId);

    /**
     * Cosine-similarity retrieval (pgvector {@code <=>}) restricted to READY, non-deleted documents
     * of the subject AND to the selected document ids (BR-07), best matches first.
     */
    List<ChunkSearchResult> searchSimilarChunks(UUID subjectId, Collection<UUID> documentIds,
                                                float[] queryEmbedding, double minSimilarity, int limit);

    void deleteByDocumentId(UUID documentId);
    void deleteByDocumentIds(Collection<UUID> documentIds);
}
