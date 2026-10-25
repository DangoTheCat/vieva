package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public record ExamQuestionView(
        UUID id,
        UUID examId,
        UUID questionId,
        UUID questionVersionId,
        int orderIndex,
        String questionContent,
        String referenceAnswer
) {}
