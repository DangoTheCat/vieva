package com.example.vieva.application.ports.input.knowledge;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class KnowledgeSearchResult {
    UUID chunkId;
    UUID documentId;
    String documentName;
    Integer chunkIndex;
    Integer pageStart;
    String content;
    double similarity;
}
