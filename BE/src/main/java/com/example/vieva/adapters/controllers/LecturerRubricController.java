package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.CriterionRequest;
import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.RubricRequest;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionVersionDto;
import com.example.vieva.adapters.presenters.QuestionBankPresenter;
import com.example.vieva.application.usecases.question.RubricService;
import com.example.vieva.domain.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * UC1.7: rubric of a DRAFT version. Responses return the whole version with its recomputed rubric.
 */
@RestController
@RequestMapping("/api/v1/lecturer/question-versions/{versionId}/rubric")
@RequiredArgsConstructor
public class LecturerRubricController {

    private final RubricService rubricService;
    private final QuestionBankPresenter presenter;

    @GetMapping
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> getRubric(@PathVariable UUID versionId) {
        return ResponseEntity.ok(presenter.toDto(rubricService.getRubric(versionId)));
    }

    @PutMapping
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> replaceRubric(@PathVariable UUID versionId,
                                                            @Valid @RequestBody RubricRequest request,
                                                            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(presenter.toDto(rubricService.replaceRubric(versionId, request.toInput(),
                request.expectedVersion(), CurrentUser.id(currentUser))));
    }

    @PostMapping("/criteria")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> addCriterion(@PathVariable UUID versionId,
                                                           @Valid @RequestBody CriterionRequest request,
                                                           @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.toDto(rubricService.addCriterion(versionId,
                request.toInput(), request.expectedVersion(), CurrentUser.id(currentUser))));
    }

    @PutMapping("/criteria/{criterionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> updateCriterion(@PathVariable UUID versionId,
                                                              @PathVariable UUID criterionId,
                                                              @Valid @RequestBody CriterionRequest request,
                                                              @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(presenter.toDto(rubricService.updateCriterion(versionId, criterionId,
                request.toInput(), request.expectedVersion(), CurrentUser.id(currentUser))));
    }

    @DeleteMapping("/criteria/{criterionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestionVersion(#versionId, authentication)")
    public ResponseEntity<QuestionVersionDto> deleteCriterion(@PathVariable UUID versionId,
                                                              @PathVariable UUID criterionId,
                                                              @RequestParam(required = false) Long expectedVersion,
                                                              @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(presenter.toDto(rubricService.deleteCriterion(versionId, criterionId,
                expectedVersion, CurrentUser.id(currentUser))));
    }
}
