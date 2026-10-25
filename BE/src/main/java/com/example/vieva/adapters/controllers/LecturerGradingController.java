package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.GradeQuestionApiRequest;
import com.example.vieva.adapters.controllers.request.OverrideScoreApiRequest;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.exam.ExamView;
import com.example.vieva.application.ports.input.grading.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lecturer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
public class LecturerGradingController {

    private final LecturerGradingService lecturerGradingService;

    @GetMapping("/grading/courses")
    public ResponseEntity<List<GradingCourseView>> getCoursesForGrading(
            @AuthenticationPrincipal ActorView currentUser
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(lecturerGradingService.getCoursesForGrading(lecturerId));
    }

    @GetMapping("/courses/{courseId}/exams")
    public ResponseEntity<List<ExamView>> getExamsForCourse(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("courseId") UUID courseId
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(lecturerGradingService.getExamsForCourse(courseId, lecturerId));
    }

    @GetMapping("/exams/{examId}/participants")
    public ResponseEntity<List<ExamParticipantView>> getExamParticipants(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("examId") UUID examId
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(lecturerGradingService.getExamParticipants(examId, lecturerId));
    }

    @GetMapping("/exams/{examId}/stats")
    public ResponseEntity<ExamGradingStatsView> getExamStats(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("examId") UUID examId
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(lecturerGradingService.getExamStats(examId, lecturerId));
    }

    @GetMapping("/courses/{courseId}/stats")
    public ResponseEntity<CourseGradingStatsView> getCourseStats(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("courseId") UUID courseId
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(lecturerGradingService.getCourseStats(courseId, lecturerId));
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AttemptGradingDetailView> getAttemptDetail(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("attemptId") UUID attemptId
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(lecturerGradingService.getAttemptDetail(attemptId, lecturerId));
    }

    @PutMapping("/attempts/{attemptId}/questions/{questionId}")
    public ResponseEntity<AttemptGradingDetailView> gradeQuestion(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("attemptId") UUID attemptId,
            @PathVariable("questionId") UUID questionId,
            @Valid @RequestBody GradeQuestionApiRequest request
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        GradeQuestionCommand command = new GradeQuestionCommand(request.score(), request.feedback());
        return ResponseEntity.ok(lecturerGradingService.gradeQuestion(attemptId, questionId, command, lecturerId));
    }

    @GetMapping("/scores")
    public ResponseEntity<List<LecturerScoreView>> listScores(
            @AuthenticationPrincipal ActorView currentUser
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(lecturerGradingService.listScores(lecturerId));
    }

    @PatchMapping("/scores/{id}")
    public ResponseEntity<LecturerScoreView> overrideScore(
            @AuthenticationPrincipal ActorView currentUser,
            @PathVariable("id") UUID id,
            @Valid @RequestBody OverrideScoreApiRequest request
    ) {
        UUID lecturerId = CurrentUser.id(currentUser);
        OverrideScoreCommand command = new OverrideScoreCommand(request.overrideTotalScore(), request.reason());
        return ResponseEntity.ok(lecturerGradingService.overrideScore(id, command, lecturerId));
    }
}
