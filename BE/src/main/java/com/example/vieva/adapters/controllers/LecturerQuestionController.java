package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.CreateManualQuestionApiRequest;
import com.example.vieva.adapters.controllers.request.GenerateQuestionsRagApiRequest;
import com.example.vieva.adapters.controllers.request.RejectVersionApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateQuestionDraftApiRequest;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.adapters.presenters.PageResponse;
import com.example.vieva.adapters.presenters.QuestionDetailDto;
import com.example.vieva.adapters.presenters.QuestionPresenter;
import com.example.vieva.application.ports.input.CreateManualQuestionRequest;
import com.example.vieva.application.ports.input.GenerateQuestionsRagRequest;
import com.example.vieva.application.ports.input.QuestionSearchCriteria;
import com.example.vieva.application.ports.input.RubricCriterionInput;
import com.example.vieva.application.ports.input.UpdateQuestionDraftRequest;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionDetailView;
import com.example.vieva.application.usecases.question.QuestionBankService;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionStatus;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/lecturer")
@RequiredArgsConstructor
public class LecturerQuestionController {

    private final QuestionBankService questionBankService;
    private final QuestionPresenter questionPresenter;

    @PostMapping("/subjects/{subjectId}/questions/generate-ai")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId)")
    public ResponseEntity<List<QuestionDetailDto>> generateQuestionsAi(
            @PathVariable UUID subjectId,
            @Valid @RequestBody GenerateQuestionsRagApiRequest apiRequest,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        GenerateQuestionsRagRequest request = GenerateQuestionsRagRequest.builder()
                .topicId(apiRequest.getTopicId())
                .bloomLevel(apiRequest.getBloomLevel())
                .quantity(apiRequest.getQuantity())
                .customPrompt(apiRequest.getCustomPrompt())
                .build();
        List<QuestionDetailView> views = questionBankService.generateQuestionsViaRag(subjectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(questionPresenter.toDtoList(views));
    }

    @PostMapping("/subjects/{subjectId}/questions/manual")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId)")
    public ResponseEntity<QuestionDetailDto> createManualQuestion(
            @PathVariable UUID subjectId,
            @Valid @RequestBody CreateManualQuestionApiRequest apiRequest,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        CreateManualQuestionRequest request = CreateManualQuestionRequest.builder()
                .topicId(apiRequest.getTopicId())
                .questionContent(apiRequest.getQuestionContent())
                .referenceAnswer(apiRequest.getReferenceAnswer())
                .bloomLevel(apiRequest.getBloomLevel())
                .rubricName(apiRequest.getRubricName())
                .totalPoints(apiRequest.getTotalPoints())
                .rubricDescription(apiRequest.getRubricDescription())
                .criteria(toCriterionInputs(apiRequest.getCriteria()))
                .build();
        QuestionDetailView view = questionBankService.createManualQuestion(subjectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(questionPresenter.toDto(view));
    }

    @GetMapping("/subjects/{subjectId}/questions")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId)")
    public ResponseEntity<PageResponse<QuestionDetailDto>> searchQuestions(
            @PathVariable UUID subjectId,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) BloomLevel bloomLevel,
            @RequestParam(required = false) QuestionApprovalStatus approvalStatus,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        QuestionSearchCriteria criteria = QuestionSearchCriteria.builder()
                .subjectId(subjectId)
                .topicId(topicId)
                .bloomLevel(bloomLevel)
                .approvalStatus(approvalStatus)
                .status(status)
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();

        PagedResult<QuestionDetailView> pagedResult = questionBankService.searchQuestions(criteria);
        return ResponseEntity.ok(questionPresenter.toPageResponse(pagedResult));
    }

    @GetMapping("/questions/{questionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId)")
    public ResponseEntity<QuestionDetailDto> getQuestionDetails(@PathVariable UUID questionId) {
        QuestionDetailView view = questionBankService.getQuestionDetails(questionId);
        return ResponseEntity.ok(questionPresenter.toDto(view));
    }

    @PostMapping("/questions/{questionId}/create-draft")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId)")
    public ResponseEntity<QuestionDetailDto> createDraft(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        QuestionDetailView view = questionBankService.createDraftFromApproved(questionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(questionPresenter.toDto(view));
    }

    @PutMapping("/questions/{questionId}/versions/{versionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId)")
    public ResponseEntity<QuestionDetailDto> updateDraftVersion(
            @PathVariable UUID questionId,
            @PathVariable UUID versionId,
            @Valid @RequestBody UpdateQuestionDraftApiRequest apiRequest,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        UpdateQuestionDraftRequest request = UpdateQuestionDraftRequest.builder()
                .questionContent(apiRequest.getQuestionContent())
                .referenceAnswer(apiRequest.getReferenceAnswer())
                .bloomLevel(apiRequest.getBloomLevel())
                .rubricName(apiRequest.getRubricName())
                .totalPoints(apiRequest.getTotalPoints())
                .rubricDescription(apiRequest.getRubricDescription())
                .criteria(toCriterionInputs(apiRequest.getCriteria()))
                .build();
        QuestionDetailView view = questionBankService.updateDraftVersion(questionId, versionId, request, userId);
        return ResponseEntity.ok(questionPresenter.toDto(view));
    }

    @PostMapping("/questions/{questionId}/versions/{versionId}/approve")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId)")
    public ResponseEntity<QuestionDetailDto> approveVersion(
            @PathVariable UUID questionId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        QuestionDetailView view = questionBankService.approveQuestionVersion(questionId, versionId, userId);
        return ResponseEntity.ok(questionPresenter.toDto(view));
    }

    @PostMapping("/questions/{questionId}/versions/{versionId}/reject")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId)")
    public ResponseEntity<QuestionDetailDto> rejectVersion(
            @PathVariable UUID questionId,
            @PathVariable UUID versionId,
            @Valid @RequestBody RejectVersionApiRequest request,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        QuestionDetailView view = questionBankService.rejectQuestionVersion(questionId, versionId, request.getReason(), userId);
        return ResponseEntity.ok(questionPresenter.toDto(view));
    }

    @DeleteMapping("/questions/{questionId}/versions/{versionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId)")
    public ResponseEntity<MessageResponse> deleteDraftVersion(
            @PathVariable UUID questionId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        questionBankService.deleteDraftVersion(questionId, versionId, userId);
        return ResponseEntity.ok(new MessageResponse("Draft version deleted successfully"));
    }

    @PatchMapping("/questions/{questionId}/archive")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId)")
    public ResponseEntity<MessageResponse> archiveQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        questionBankService.archiveQuestion(questionId, userId);
        return ResponseEntity.ok(new MessageResponse("Question archived successfully"));
    }

    @PatchMapping("/questions/{questionId}/restore")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId)")
    public ResponseEntity<MessageResponse> restoreQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        questionBankService.restoreQuestion(questionId, userId);
        return ResponseEntity.ok(new MessageResponse("Question restored successfully"));
    }

    private UUID resolveUserId(User currentUser) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return currentUser.getUserId();
    }

    private List<RubricCriterionInput> toCriterionInputs(
            List<com.example.vieva.adapters.controllers.request.RubricCriterionApiRequest> apiCriteria) {
        if (apiCriteria == null) {
            return List.of();
        }
        return apiCriteria.stream()
                .map(c -> RubricCriterionInput.builder()
                        .criterionName(c.getCriterionName())
                        .maxPoints(c.getMaxPoints())
                        .achievementDescriptors(c.getAchievementDescriptors())
                        .orderIndex(c.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
    }
}
