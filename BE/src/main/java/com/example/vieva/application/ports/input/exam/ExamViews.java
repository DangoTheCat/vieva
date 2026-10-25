package com.example.vieva.application.ports.input.exam;

import com.example.vieva.domain.entities.ExamStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ExamViews {

    public record ExamView(
            UUID id,
            UUID subjectId,
            UUID creatorId,
            String title,
            int durationSeconds,
            ExamStatus status,
            Instant createdAt,
            Instant updatedAt,
            int questionCount
    ) {}

    public record ExamQuestionItemView(
            UUID id,
            UUID questionId,
            UUID questionVersionId,
            int orderIndex,
            String content,
            String referenceAnswer
    ) {}

    public record ExamDetailView(
            UUID id,
            UUID subjectId,
            UUID creatorId,
            String title,
            int durationSeconds,
            ExamStatus status,
            Instant createdAt,
            Instant updatedAt,
            List<ExamQuestionItemView> questions
    ) {}

    public record StudentQuestionView(
            UUID questionId,
            int orderIndex,
            String questionText
    ) {}

    public record StudentScoreView(
            UUID attemptId,
            UUID examId,
            String examTitle,
            java.math.BigDecimal totalScore,
            com.example.vieva.domain.entities.GradeLevel gradeLevel,
            Instant completedAt
    ) {}
}
