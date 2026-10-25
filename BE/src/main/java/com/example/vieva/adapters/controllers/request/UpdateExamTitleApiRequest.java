package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateExamTitleApiRequest(
        @NotBlank(message = "Exam title cannot be blank")
        String title
) {}
