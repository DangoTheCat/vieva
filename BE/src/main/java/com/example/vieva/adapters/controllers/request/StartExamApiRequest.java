package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartExamApiRequest(
        @NotNull(message = "subjectId is required")
        UUID subjectId
) {
}
