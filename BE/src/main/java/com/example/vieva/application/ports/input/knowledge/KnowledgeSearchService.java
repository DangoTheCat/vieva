package com.example.vieva.application.ports.input.knowledge;

import java.util.List;
import java.util.UUID;

public interface KnowledgeSearchService {
    KnowledgeSearchResult createKnowledge(UUID subjectId, String content, UUID topicId, UUID actorId);
    List<KnowledgeSearchResult> listReadyChunks(UUID subjectId, int limit);
    List<KnowledgeSearchResult> searchKnowledge(UUID subjectId, String query, int limit);
}
