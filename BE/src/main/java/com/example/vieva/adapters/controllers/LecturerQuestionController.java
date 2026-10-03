package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.CreateManualQuestionRequest;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.adapters.presenters.PageResponse;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionBankItemDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionDetailDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionVersionDto;
import com.example.vieva.adapters.presenters.QuestionBankPresenter;
import com.example.vieva.application.ports.input.CreateManualQuestionCommand;
import com.example.vieva.application.ports.input.QuestionBankSearchCriteria;
import com.example.vieva.application.usecases.question.QuestionAuthoringService;
import com.example.vieva.application.usecases.question.QuestionBankService;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * UC1.5 (manual create) and UC1.4 (bank search, detail, history, archive, Copy-on-Write draft).
 */
@RestController
@RequestMapping("/api/v1/lecturer")
@RequiredArgsConstructor
public class LecturerQuestionController {

    private final QuestionAuthoringService authoringService;
    private final QuestionBankService bankService;
    private final QuestionBankPresenter presenter;

    @PostMapping("/subjects/{subjectId}/questions")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<QuestionVersionDto> createManualQuestion(
            @PathVariable UUID subjectId,
            @Valid @RequestBody CreateManualQuestionRequest request,
            @AuthenticationPrincipal User currentUser) {
        CreateManualQuestionCommand command = CreateManualQuestionCommand.builder()
                .topicId(request.topicId())
                .content(request.content())
                .expectedAnswer(request.expectedAnswer())
                .bloomLevel(request.bloomLevel())
                .rubric(request.rubric().toInput())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(presenter.toDto(authoringService.createManualQuestion(subjectId, command, CurrentUser.id(currentUser))));
    }

    /** Official bank: questions with an APPROVED version only. */
    @GetMapping("/subjects/{subjectId}/questions")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<PageResponse<QuestionBankItemDto>> searchBank(
            @PathVariable UUID subjectId,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) BloomLevel bloomLevel,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        QuestionBankSearchCriteria criteria = QuestionBankSearchCriteria.builder()
                .subjectId(subjectId)
                .topicId(topicId)
                .bloomLevel(bloomLevel)
                .status(status)
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortAscending("asc".equalsIgnoreCase(sortDir))
                .build();
        return ResponseEntity.ok(presenter.toBankPage(bankService.search(criteria)));
    }

    @GetMapping("/questions/{questionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId, authentication)")
    public ResponseEntity<QuestionDetailDto> getQuestion(@PathVariable UUID questionId) {
        return ResponseEntity.ok(presenter.toDetail(bankService.getDetail(questionId)));
    }

    @GetMapping("/questions/{questionId}/versions")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId, authentication)")
    public ResponseEntity<List<QuestionVersionDto>> getHistory(@PathVariable UUID questionId) {
        return ResponseEntity.ok(presenter.toDtoList(bankService.getHistory(questionId)));
    }

    /** Copy-on-Write: new DRAFT from the approved version in force. */
    @PostMapping("/questions/{questionId}/versions")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId, authentication)")
    public ResponseEntity<QuestionVersionDto> createDraftVersion(@PathVariable UUID questionId,
                                                                 @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(presenter.toDto(authoringService.createDraftFromApproved(questionId, CurrentUser.id(currentUser))));
    }

    @PostMapping("/questions/{questionId}/archive")
    @PreAuthorize("@courseSecurityEvaluator.canAccessQuestion(#questionId, authentication)")
    public ResponseEntity<MessageResponse> archiveQuestion(@PathVariable UUID questionId,
                                                           @AuthenticationPrincipal User currentUser) {
        bankService.archive(questionId, CurrentUser.id(currentUser));
        return ResponseEntity.ok(new MessageResponse("Question archived"));
    }
}
