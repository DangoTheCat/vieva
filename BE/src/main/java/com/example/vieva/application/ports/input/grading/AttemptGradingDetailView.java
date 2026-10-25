package com.example.vieva.application.ports.input.grading;

import com.example.vieva.domain.entities.AttemptStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AttemptGradingDetailView(
        UUID attemptId,
        UUID examId,
        String examTitle,
        UUID studentId,
        String studentName,
        String studentEmail,
        AttemptStatus status,
        BigDecimal totalScore,
        BigDecimal aiScore,
        BigDecimal teacherScore,
        BigDecimal overrideTotalScore,
        List<QuestionGradingView> questions
) {}
