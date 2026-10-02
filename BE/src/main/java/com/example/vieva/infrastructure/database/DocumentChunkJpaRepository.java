package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkJpaRepository extends JpaRepository<DocumentChunkJpaEntity, UUID> {

    List<DocumentChunkJpaEntity> findByDocument_DocumentIdOrderByChunkIndexAsc(UUID documentId);

    @Modifying
    @Query("DELETE FROM DocumentChunkJpaEntity c WHERE c.document.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    @Query(value = """
        SELECT c.chunk_id AS chunkId,
               c.document_id AS documentId,
               d.file_name AS documentName,
               c.chunk_index AS chunkIndex,
               c.content AS content,
               (c.embedding <=> cast(:queryVector as vector)) AS distance
        FROM document_chunks c
        JOIN course_documents d ON c.document_id = d.document_id
        WHERE d.subject_id = :subjectId
          AND d.indexing_status = 'READY'
          AND d.deleted_at IS NULL
          AND (c.embedding <=> cast(:queryVector as vector)) <= :maxDistance
        ORDER BY distance ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<ChunkSearchResultProjection> searchSimilarChunks(
            @Param("subjectId") UUID subjectId,
            @Param("queryVector") String queryVector,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit);
}
