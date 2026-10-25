package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SetExamQuestionsApiRequest(
        @NotEmpty(message = "Questions list cannot be empty")
        List<ExamQuestionItemApiRequest> questions
) {
    public record ExamQuestionItemApiRequest(
            @NotNull(message = "Question ID is required")
            UUID questionId,

            @NotNull(message = "Question version ID is required")
            UUID questionVersionId,

            @NotNull(message = "Order index is required")
            Integer orderIndex
    ) {}
}
