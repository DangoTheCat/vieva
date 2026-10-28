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

        // 1 minute to 4 hours
        @Min(value = 60, message = "Exam duration must be at least 1 minute")
        @Max(value = 14400, message = "Exam duration must not exceed 240 minutes")
        Integer durationSeconds
) {}
