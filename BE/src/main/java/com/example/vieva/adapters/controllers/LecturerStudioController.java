package com.example.vieva.adapters.controllers;

import com.example.vieva.application.usecases.document.DocumentOperations;
import com.example.vieva.application.usecases.generation.QuestionGenerationOperations;
import com.example.vieva.application.usecases.question.QuestionReviewService;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.adapters.presenters.*;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionVersionDto;
import com.example.vieva.domain.entities.*;
import com.example.vieva.domain.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/v1/lecturer/studio")
@RequiredArgsConstructor
public class LecturerStudioController {
    private final DocumentOperations docService;
    private final QuestionGenerationOperations ragCoord;
    private final QuestionReviewService reviews;
    private final QuestionVersionRepository versions;
    private final QuestionBankPresenter presenter;
    private final CourseDocumentPresenter documentPresenter;

    public record GenerateRequest(@NotNull UUID topicId, @NotNull BloomLevel difficulty,
                                  @Size(max=20) List<@NotNull UUID> documentIds) { }
    public record ReviewRequest(@NotNull UUID versionId, @NotNull Long expectedVersion) { }
    public record RejectRequest(@NotNull UUID versionId, @NotNull Long expectedVersion,
                                @NotBlank @Size(max=2000) String reason) { }
    public record Preview(QuestionVersionDto question, BloomLevel difficulty, Object rubricJson) { }

    @PostMapping(value="/subjects/{subjectId}/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseDocumentDto> uploadDocument(@PathVariable UUID subjectId,
            @RequestPart MultipartFile file,@AuthenticationPrincipal User user) throws IOException {
        var document=docService.processDocumentUpload(subjectId,file.getOriginalFilename(),file.getBytes(),CurrentUser.id(user));
        return ResponseEntity.accepted().body(documentPresenter.toDto(document));
    }

    @PostMapping("/questions/generate")
    public ResponseEntity<Preview> generateQuestion(@Valid @RequestBody GenerateRequest request,
                                                    @AuthenticationPrincipal User user) {
        var view=ragCoord.coordinateRAGPipeline(request.topicId(),request.difficulty(),request.documentIds(),CurrentUser.id(user));
        var dto=presenter.toDto(view);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Preview(dto,request.difficulty(),dto.rubric()));
    }

    @PostMapping("/questions/{questionId}/submit-for-review")
    public QuestionVersionDto submitForReview(@PathVariable UUID questionId,@Valid @RequestBody ReviewRequest r,
                                               @AuthenticationPrincipal User user) {
        checkVersion(questionId,r.versionId());
        return presenter.toDto(reviews.submitForReview(r.versionId(),r.expectedVersion(),CurrentUser.id(user)));
    }

    @GetMapping("/questions/{questionId}/review")
    public QuestionVersionDto reviewQuestion(@PathVariable UUID questionId,@RequestParam UUID versionId,
                                              @AuthenticationPrincipal User user) {
        checkVersion(questionId,versionId);
        return presenter.toDto(reviews.reviewQuestion(versionId,CurrentUser.id(user)));
    }

    @PostMapping("/questions/{questionId}/approve")
    public QuestionVersionDto approveQuestion(@PathVariable UUID questionId,@Valid @RequestBody ReviewRequest r,
                                               @AuthenticationPrincipal User user) {
        checkVersion(questionId,r.versionId());
        return presenter.toDto(reviews.approve(r.versionId(),r.expectedVersion(),CurrentUser.id(user)));
    }

    @PostMapping("/questions/{questionId}/reject")
    public QuestionVersionDto rejectQuestion(@PathVariable UUID questionId,@Valid @RequestBody RejectRequest r,
                                              @AuthenticationPrincipal User user) {
        checkVersion(questionId,r.versionId());
        return presenter.toDto(reviews.reject(r.versionId(),r.reason(),r.expectedVersion(),CurrentUser.id(user)));
    }

    @PostMapping("/documents/{documentId}/archive")
    public void archiveDocument(@PathVariable UUID documentId,@AuthenticationPrincipal User user) {
        docService.archiveDocument(documentId,CurrentUser.id(user));
    }

    private void checkVersion(UUID questionId,UUID versionId) {
        var version=versions.findById(versionId).orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND));
        if (!questionId.equals(version.getQuestionId())) throw new AppException(ErrorCode.INVALID_REQUEST,"Version belongs to another question");
    }
}
