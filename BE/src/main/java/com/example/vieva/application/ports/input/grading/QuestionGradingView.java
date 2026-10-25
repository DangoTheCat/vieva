package com.example.vieva.application.ports.input.grading;

import com.example.vieva.application.ports.input.exam.ExamTurnDetailView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuestionGradingView(
        UUID questionId,
        UUID questionVersionId,
        int orderIndex,
        String questionText,
        String expectedAnswer,
        BigDecimal effectiveScore,
        BigDecimal teacherScore,
        String teacherFeedback,
        List<ExamTurnDetailView> turns
) {}
