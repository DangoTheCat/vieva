package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Các đoạn chunk văn bản phục vụ truy hồi ngữ cảnh semantic search.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    private UUID chunkId;
    private UUID documentId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private String vectorId;
    private float[] embedding;
    private String metadataJson;
    private Instant createdAt;
}
