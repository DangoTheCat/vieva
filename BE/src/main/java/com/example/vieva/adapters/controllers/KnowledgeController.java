package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchResult;
import com.example.vieva.application.ports.input.knowledge.KnowledgeSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeSearchService knowledgeSearchService;

    public record CreateKnowledgeRequest(
            @NotNull(message = "Subject ID is required")
            UUID subjectId,

            @NotBlank(message = "Content is required")
            String content,

            UUID topicId
    ) {}

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KnowledgeSearchResult>> getKnowledge(
            @RequestParam UUID subjectId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(knowledgeSearchService.listReadyChunks(subjectId, limit));
        }
        return ResponseEntity.ok(knowledgeSearchService.searchKnowledge(subjectId, q.trim(), limit));
    }

    @PostMapping
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#request.subjectId(), authentication) and hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<KnowledgeSearchResult> createKnowledge(
            @Valid @RequestBody CreateKnowledgeRequest request,
            @AuthenticationPrincipal ActorView currentUser
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        KnowledgeSearchResult result = knowledgeSearchService.createKnowledge(
                request.subjectId(),
                request.content(),
                request.topicId(),
                actorId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
