package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public record StudentFrozenQuestionView(
        UUID questionId,
        UUID questionVersionId,
        int orderIndex,
        String questionText
) {}
