package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.CreateExamApiRequest;
import com.example.vieva.adapters.controllers.request.SetExamQuestionsApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateExamStatusApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateExamTitleApiRequest;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.exam.*;
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
@RequestMapping("/api/v1/lecturer/exams")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
public class LecturerExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ExamView> createExam(
            @AuthenticationPrincipal ActorView currentUser,
            @Valid @RequestBody CreateExamApiRequest request
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        CreateExamCommand command = new CreateExamCommand(request.subjectId(), request.title(), request.durationSeconds(),
                request.answerTimeLimitSeconds(), request.maxFollowUpsPerQuestion());
        ExamView view = examService.createExam(command, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping
    public ResponseEntity<List<ExamView>> listExams(
            @AuthenticationPrincipal ActorView currentUser,
            @RequestParam(name = "subjectId") UUID subjectId
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(examService.listExamsForSubject(subjectId, actorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamDetailView> getExam(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(examService.getExam(id, actorId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExamView> updateExamTitle(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateExamTitleApiRequest request
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        UpdateExamCommand command = new UpdateExamCommand(request.title(), request.answerTimeLimitSeconds(),
                request.maxFollowUpsPerQuestion());
        return ResponseEntity.ok(examService.updateExamTitle(id, command, actorId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ExamView> updateExamStatus(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateExamStatusApiRequest request
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        UpdateExamStatusCommand command = new UpdateExamStatusCommand(request.status());
        return ResponseEntity.ok(examService.updateExamStatus(id, command, actorId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        examService.deleteExam(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/questions")
    public ResponseEntity<ExamDetailView> setExamQuestions(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id,
            @Valid @RequestBody SetExamQuestionsApiRequest request
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        List<SetExamQuestionsCommand.ExamQuestionItem> items = request.questions().stream()
                .map(q -> new SetExamQuestionsCommand.ExamQuestionItem(q.questionId(), q.questionVersionId(), q.orderIndex()))
                .collect(Collectors.toList());
        SetExamQuestionsCommand command = new SetExamQuestionsCommand(items);
        return ResponseEntity.ok(examService.setExamQuestions(id, command, actorId));
    }
}
