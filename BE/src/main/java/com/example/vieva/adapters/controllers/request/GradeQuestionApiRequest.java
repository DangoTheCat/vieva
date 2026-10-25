package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeQuestionApiRequest(
        @NotNull(message = "Score is required")
        @DecimalMin(value = "0.0", message = "Score must be at least 0.0")
        @DecimalMax(value = "10.0", message = "Score cannot exceed 10.0")
        BigDecimal score,

        String feedback
) {}
