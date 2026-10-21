package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.StartExamApiRequest;
import com.example.vieva.adapters.presenters.ExamAttemptPresenter;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.exam.ExamAttemptDetailView;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.GetExamAttemptUseCase;
import com.example.vieva.application.ports.input.exam.StartExamUseCase;
import com.example.vieva.application.ports.input.exam.SubmitTurnAnswerUseCase;
import com.example.vieva.application.ports.input.exam.TurnEvaluationView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/student/attempts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamAttemptController {

    private final StartExamUseCase startExamUseCase;
    private final GetExamAttemptUseCase getExamAttemptUseCase;
    private final SubmitTurnAnswerUseCase submitTurnAnswerUseCase;
    private final ExamAttemptPresenter presenter;

    @PostMapping
    public ResponseEntity<ExamAttemptPresenter.ExamAttemptResponse> startExam(
            @AuthenticationPrincipal ActorView currentUser,
            @Valid @RequestBody StartExamApiRequest request
    ) {
        UUID studentId = CurrentUser.id(currentUser);
        ExamAttemptView attemptView = startExamUseCase.startExam(studentId, request.subjectId());
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.toAttemptResponse(attemptView));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamAttemptPresenter.ExamAttemptDetailResponse> getAttemptDetail(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id
    ) {
        UUID studentId = CurrentUser.id(currentUser);
        ExamAttemptDetailView detailView = getExamAttemptUseCase.getAttemptDetail(id, studentId);
        return ResponseEntity.ok(presenter.toAttemptDetailResponse(detailView));
    }

    @PostMapping(value = "/{id}/turns/{turnId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExamAttemptPresenter.TurnEvaluationResponse> submitTurnMultipart(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id,
            @PathVariable("turnId") UUID turnId,
            @RequestParam(value = "transcriptText", required = false) String transcriptText,
            @RequestPart(value = "audioFile", required = false) MultipartFile audioFile
    ) {
        UUID studentId = CurrentUser.id(currentUser);
        byte[] audioBytes = null;
        String contentType = null;

        if (audioFile != null && !audioFile.isEmpty()) {
            try {
                audioBytes = audioFile.getBytes();
                contentType = audioFile.getContentType();
            } catch (IOException e) {
                log.warn("Failed to read audio file bytes: {}", e.getMessage());
            }
        }

        TurnEvaluationView evaluationView = submitTurnAnswerUseCase.submitAnswer(
                id, turnId, studentId, transcriptText, audioBytes, contentType
        );
        return ResponseEntity.ok(presenter.toTurnEvaluationResponse(evaluationView));
    }

    public record SubmitTurnJsonPayload(
            String transcriptText
    ) {}

    @PostMapping(value = "/{id}/turns/{turnId}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExamAttemptPresenter.TurnEvaluationResponse> submitTurnJson(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id,
            @PathVariable("turnId") UUID turnId,
            @RequestBody SubmitTurnJsonPayload payload
    ) {
        UUID studentId = CurrentUser.id(currentUser);
        String transcript = payload != null ? payload.transcriptText() : null;

        TurnEvaluationView evaluationView = submitTurnAnswerUseCase.submitAnswer(
                id, turnId, studentId, transcript, null, null
        );
        return ResponseEntity.ok(presenter.toTurnEvaluationResponse(evaluationView));
    }
}
