package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateExamApiRequest(
        @NotNull(message = "Subject ID is required")
        UUID subjectId,

        @NotBlank(message = "Exam title is required")
        String title,

        Integer durationSeconds,

        @Min(value = 30, message = "Answer time limit must be at least 30 seconds")
        @Max(value = 600, message = "Answer time limit must be at most 600 seconds")
        Integer answerTimeLimitSeconds,

        @Min(value = 0, message = "Max follow-ups must be between 0 and 3")
        @Max(value = 3, message = "Max follow-ups must be between 0 and 3")
        Integer maxFollowUpsPerQuestion
) {}
