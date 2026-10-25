package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchResult;
import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeControllerTest {

    @Mock
    private KnowledgeSearchService knowledgeSearchService;

    private KnowledgeController controller;

    @BeforeEach
    void setUp() {
        controller = new KnowledgeController(knowledgeSearchService);
    }

    @Test
    @DisplayName("GET /api/v1/knowledge lists ready chunks when q is absent")
    void getKnowledgeListsReadyChunksWhenQueryAbsent() {
        UUID subjectId = UUID.randomUUID();
        KnowledgeSearchResult item = KnowledgeSearchResult.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .documentName("Doc1.pdf")
                .chunkIndex(0)
                .content("Sample content")
                .similarity(1.0)
                .build();

        when(knowledgeSearchService.listReadyChunks(eq(subjectId), eq(10)))
                .thenReturn(List.of(item));

        ResponseEntity<List<KnowledgeSearchResult>> response = controller.getKnowledge(subjectId, null, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(item);
        verify(knowledgeSearchService).listReadyChunks(subjectId, 10);
    }

    @Test
    @DisplayName("GET /api/v1/knowledge searches knowledge when q is provided")
    void getKnowledgeSearchesWhenQueryProvided() {
        UUID subjectId = UUID.randomUUID();
        KnowledgeSearchResult item = KnowledgeSearchResult.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .documentName("Doc1.pdf")
                .content("Matching query content")
                .similarity(0.85)
                .build();

        when(knowledgeSearchService.searchKnowledge(eq(subjectId), eq("test query"), eq(5)))
                .thenReturn(List.of(item));

        ResponseEntity<List<KnowledgeSearchResult>> response = controller.getKnowledge(subjectId, "test query", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(item);
        verify(knowledgeSearchService).searchKnowledge(subjectId, "test query", 5);
    }

    @Test
    @DisplayName("POST /api/v1/knowledge creates knowledge and returns 201 Created")
    void createKnowledgeReturnsCreated() {
        UUID subjectId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        ActorView actor = new ActorView(actorId, "lec@vieva.edu.vn", Set.of("ROLE_LECTURER"), false);
        KnowledgeController.CreateKnowledgeRequest request =
                new KnowledgeController.CreateKnowledgeRequest(subjectId, "Architecture note", topicId);

        KnowledgeSearchResult result = KnowledgeSearchResult.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .documentName("Knowledge-1.txt")
                .content("Architecture note")
                .similarity(1.0)
                .build();

        when(knowledgeSearchService.createKnowledge(eq(subjectId), eq("Architecture note"), eq(topicId), eq(actorId)))
                .thenReturn(result);

        ResponseEntity<KnowledgeSearchResult> response = controller.createKnowledge(request, actor);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(result);
    }
}
