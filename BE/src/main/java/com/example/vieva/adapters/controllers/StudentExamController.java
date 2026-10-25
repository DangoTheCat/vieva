package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.presenters.ExamAttemptPresenter;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.StudentExamService;
import com.example.vieva.application.ports.input.exam.StudentExamView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {

    private final StudentExamService studentExamService;
    private final ExamAttemptPresenter presenter;

    @GetMapping
    public ResponseEntity<List<StudentExamView>> listActiveExams(@AuthenticationPrincipal ActorView currentUser) {
        UUID studentId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(studentExamService.listActiveExams(studentId));
    }

    @PostMapping("/{id}/enter")
    public ResponseEntity<ExamAttemptPresenter.ExamAttemptResponse> enterExam(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id
    ) {
        UUID studentId = CurrentUser.id(currentUser);
        ExamAttemptView attemptView = studentExamService.enterExam(id, studentId);
        return ResponseEntity.ok(presenter.toAttemptResponse(attemptView));
    }
}
