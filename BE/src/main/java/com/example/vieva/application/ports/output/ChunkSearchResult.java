package com.example.vieva.application.ports.output;

import java.util.UUID;

/**
 * One retrieved chunk. {@code similarityScore} = 1 - cosine distance, clamped to [0, 1].
 */
public record ChunkSearchResult(
    UUID chunkId,
    UUID documentId,
    String documentName,
    Integer chunkIndex,
    Integer page,
    String content,
    double distance,
    double similarityScore
) {
    public static ChunkSearchResult of(UUID chunkId, UUID documentId, String documentName,
                                       Integer chunkIndex, Integer page, String content, double distance) {
        double similarity = Math.max(0.0, Math.min(1.0, 1.0 - distance));
        return new ChunkSearchResult(chunkId, documentId, documentName, chunkIndex, page, content, distance, similarity);
    }
}
