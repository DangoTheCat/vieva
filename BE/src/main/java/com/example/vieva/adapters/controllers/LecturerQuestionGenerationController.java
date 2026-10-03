package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.GenerateQuestionsRequest;
import com.example.vieva.adapters.presenters.QuestionBankDtos.GenerationRequestDto;
import com.example.vieva.adapters.presenters.QuestionBankPresenter;
import com.example.vieva.application.ports.input.GenerateQuestionsCommand;
import com.example.vieva.application.usecases.generation.QuestionGenerationService;
import com.example.vieva.domain.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * UC1.2: RAG generation runs. The response always describes the persisted request: status
 * COMPLETED / PARTIAL / FAILED, counts, validation issues and the drafts created by this run.
 */
@RestController
@RequestMapping("/api/v1/lecturer/question-generation-requests")
@RequiredArgsConstructor
public class LecturerQuestionGenerationController {

    private final QuestionGenerationService generationService;
    private final QuestionBankPresenter presenter;

    @PostMapping
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#request.subjectId(), authentication)")
    public ResponseEntity<GenerationRequestDto> generate(@Valid @RequestBody GenerateQuestionsRequest request,
                                                         @AuthenticationPrincipal User currentUser) {
        GenerateQuestionsCommand command = GenerateQuestionsCommand.builder()
                .subjectId(request.subjectId())
                .topicId(request.topicId())
                .documentIds(request.documentIds())
                .totalQuestions(request.totalQuestions())
                .bloomDistribution(request.bloomDistribution())
                .lecturerNote(request.lecturerNote())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(presenter.toGeneration(generationService.generate(command, CurrentUser.id(currentUser))));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessGenerationRequest(#requestId, authentication)")
    public ResponseEntity<GenerationRequestDto> getRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(presenter.toGeneration(generationService.getRequest(requestId)));
    }

    @PostMapping("/{requestId}/retry")
    @PreAuthorize("@courseSecurityEvaluator.canAccessGenerationRequest(#requestId, authentication)")
    public ResponseEntity<GenerationRequestDto> retry(@PathVariable UUID requestId,
                                                      @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(presenter.toGeneration(generationService.retry(requestId, CurrentUser.id(currentUser))));
    }
}
