package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.NotBlank;

/** status: DRAFT | IN_PROGRESS | COMPLETED — parsed and validated by the use case. */
public record UpdateExamStatusApiRequest(
        @NotBlank(message = "Exam status is required")
        String status
) {}
