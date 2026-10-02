package com.example.vieva.application.ports.output;

import java.util.UUID;

public record ChunkSearchResult(
    UUID chunkId,
    UUID documentId,
    String documentName,
    Integer chunkIndex,
    String content,
    double distance,
    double similarityScore
) {
    public static ChunkSearchResult of(UUID chunkId, UUID documentId, String documentName,
                                      Integer chunkIndex, String content, double distance) {
        // cosine similarity = 1.0 - distance
        double similarity = Math.max(0.0, Math.min(1.0, 1.0 - distance));
        return new ChunkSearchResult(chunkId, documentId, documentName, chunkIndex, content, distance, similarity);
    }
}
