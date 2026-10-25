package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateExamApiRequest(
        @NotNull(message = "Subject ID is required")
        UUID subjectId,

        @NotBlank(message = "Exam title is required")
        String title,

        Integer durationSeconds
) {}
