package com.example.vieva.adapters.controllers.request;

import com.example.vieva.domain.entities.ExamStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateExamStatusApiRequest(
        @NotNull(message = "Exam status is required")
        ExamStatus status
) {}
