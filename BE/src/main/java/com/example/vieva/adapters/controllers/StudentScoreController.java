package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.exam.StudentExamService;
import com.example.vieva.application.ports.input.exam.StudentScoreView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/scores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentScoreController {

    private final StudentExamService studentExamService;

    @GetMapping
    public ResponseEntity<List<StudentScoreView>> getStudentScores(@AuthenticationPrincipal ActorView currentUser) {
        UUID studentId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(studentExamService.getStudentScores(studentId));
    }
}
