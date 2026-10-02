package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.ApproveRequest;
import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.ConfirmBloomRequest;
import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.RegenerateRequest;
import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.RejectRequest;
import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.UpdateDraftRequest;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.adapters.presenters.PageResponse;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionVersionDto;
import com.example.vieva.adapters.presenters.QuestionBankPresenter;
import com.example.vieva.application.ports.input.QuestionVersionSearchCriteria;
import com.example.vieva.application.ports.input.UpdateDraftCommand;
import com.example.vieva.application.usecases.generation.QuestionGenerationService;
import com.example.vieva.application.usecases.question.QuestionAuthoringService;
import com.example.vieva.application.usecases.question.QuestionReviewService;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * UC1.3 review queue and draft editing (WF01 steps 8-9), plus single-question regeneration (UC1.2).
 */
@RestController
@RequestMapping("/api/v1/lecturer/question-versions")
@RequiredArgsConstructor
public class LecturerQuestionVersionController {

    private final QuestionReviewService reviewService;
    private final QuestionAuthoringService authoringService;
    private final QuestionGenerationService generationService;
    private final QuestionBankPresenter presenter;

    @GetMapping
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<PageResponse<QuestionVersionDto>> searchVersions(
            @RequestParam UUID subjectId,
            @RequestParam(required = false) QuestionApprovalStatus status,
            @RequestParam(required = false) QuestionGenerationMode origin,
            @RequestParam(required = false) BloomLevel bloomLevel,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) UUID generationRequestId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        QuestionVersionSearchCriteria criteria = QuestionVersionSearchCriteria.builder()
                .subjectId(subjectId)
                .status(status)
                .origin(origin)
                .bloomLevel(bloomLevel)
                .topicId(topicId)
                .generationRequestId(generationRequestId)
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();
        return ResponseEntity.ok(presenter.toVersionPage(reviewService.searchVersions(criteria)));
    }

    @GetMapping("/{versionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> getVersion(@PathVariable UUID versionId) {
        return ResponseEntity.ok(presenter.toDto(reviewService.getVersion(versionId)));
    }

    @PutMapping("/{versionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> updateDraft(@PathVariable UUID versionId,
                                                          @Valid @RequestBody UpdateDraftRequest request,
                                                          @AuthenticationPrincipal User currentUser) {
        UpdateDraftCommand command = UpdateDraftCommand.builder()
                .topicId(request.topicId())
                .content(request.content())
                .expectedAnswer(request.expectedAnswer())
                .bloomLevel(request.bloomLevel())
                .bloomConfirmed(request.bloomConfirmed())
                .rubric(request.rubric() == null ? null : request.rubric().toInput())
                .expectedVersion(request.expectedVersion())
                .build();
        return ResponseEntity.ok(presenter.toDto(authoringService.updateDraft(versionId, command, CurrentUser.id(currentUser))));
    }

    @PostMapping("/{versionId}/confirm-bloom")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> confirmBloom(@PathVariable UUID versionId,
                                                           @Valid @RequestBody ConfirmBloomRequest request,
                                                           @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(presenter.toDto(authoringService.confirmBloom(versionId, request.bloomLevel(),
                request.expectedVersion(), CurrentUser.id(currentUser))));
    }

    @PostMapping("/{versionId}/approve")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> approve(@PathVariable UUID versionId,
                                                      @RequestBody(required = false) ApproveRequest request,
                                                      @AuthenticationPrincipal User currentUser) {
        Long expected = request == null ? null : request.expectedVersion();
        return ResponseEntity.ok(presenter.toDto(reviewService.approve(versionId, expected, CurrentUser.id(currentUser))));
    }

    @PostMapping("/{versionId}/reject")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> reject(@PathVariable UUID versionId,
                                                     @Valid @RequestBody RejectRequest request,
                                                     @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(presenter.toDto(reviewService.reject(versionId, request.reason(),
                request.expectedVersion(), CurrentUser.id(currentUser))));
    }

    /** Hard delete of a DRAFT only; drafts are never used in exams. */
    @DeleteMapping("/{versionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<MessageResponse> deleteDraft(@PathVariable UUID versionId,
                                                       @AuthenticationPrincipal User currentUser) {
        authoringService.deleteDraft(versionId, CurrentUser.id(currentUser));
        return ResponseEntity.ok(new MessageResponse("Draft deleted"));
    }

    @PostMapping("/{versionId}/regenerate")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> regenerate(@PathVariable UUID versionId,
                                                         @Valid @RequestBody(required = false) RegenerateRequest request,
                                                         @AuthenticationPrincipal User currentUser) {
        String feedback = request == null ? null : request.feedback();
        Long expected = request == null ? null : request.expectedVersion();
        return ResponseEntity.ok(presenter.toDto(generationService.regenerate(versionId, feedback, expected,
                CurrentUser.id(currentUser))));
    }
}
