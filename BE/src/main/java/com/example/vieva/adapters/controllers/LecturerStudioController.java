package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.document.DocumentService;
import com.example.vieva.application.ports.input.generation.QuestionGenCoordinator;
import com.example.vieva.application.ports.input.question.QuestionReviewService;
import com.example.vieva.adapters.presenters.*;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionVersionDto;
import com.example.vieva.domain.entities.*;
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
    private final DocumentService docService;
    private final QuestionGenCoordinator ragCoord;
    private final QuestionReviewService reviews;
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
            @RequestPart MultipartFile file,@AuthenticationPrincipal ActorView user) throws IOException {
        var document=docService.processDocumentUpload(subjectId,file.getOriginalFilename(),file.getBytes(),CurrentUser.id(user));
        return ResponseEntity.accepted().body(documentPresenter.toDto(document));
    }

    @PostMapping("/questions/generate")
    public ResponseEntity<Preview> generateQuestion(@Valid @RequestBody GenerateRequest request,
                                                    @AuthenticationPrincipal ActorView user) {
        var view=ragCoord.coordinateRAGPipeline(request.topicId(),request.difficulty(),request.documentIds(),CurrentUser.id(user));
        var dto=presenter.toDto(view);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Preview(dto,request.difficulty(),dto.rubric()));
    }

    @PostMapping("/questions/{questionId}/submit-for-review")
    public QuestionVersionDto submitForReview(@PathVariable UUID questionId,@Valid @RequestBody ReviewRequest r,
                                               @AuthenticationPrincipal ActorView user) {
        return presenter.toDto(reviews.submitForReview(questionId,r.versionId(),r.expectedVersion(),CurrentUser.id(user)));
    }

    @GetMapping("/questions/{questionId}/review")
    public QuestionVersionDto reviewQuestion(@PathVariable UUID questionId,@RequestParam UUID versionId,
                                              @AuthenticationPrincipal ActorView user) {
        return presenter.toDto(reviews.reviewQuestion(questionId,versionId,CurrentUser.id(user)));
    }

    @PostMapping("/questions/{questionId}/approve")
    public QuestionVersionDto approveQuestion(@PathVariable UUID questionId,@Valid @RequestBody ReviewRequest r,
                                               @AuthenticationPrincipal ActorView user) {
        return presenter.toDto(reviews.approve(questionId,r.versionId(),r.expectedVersion(),CurrentUser.id(user)));
    }

    @PostMapping("/questions/{questionId}/reject")
    public QuestionVersionDto rejectQuestion(@PathVariable UUID questionId,@Valid @RequestBody RejectRequest r,
                                               @AuthenticationPrincipal ActorView user) {
        return presenter.toDto(reviews.reject(questionId,r.versionId(),r.reason(),r.expectedVersion(),CurrentUser.id(user)));
    }

    @PostMapping("/documents/{documentId}/archive")
    public void archiveDocument(@PathVariable UUID documentId,@AuthenticationPrincipal ActorView user) {
        docService.archiveDocument(documentId,CurrentUser.id(user));
    }
}
