package com.example.vieva.application.ports.input.grading;

import java.math.BigDecimal;

public record GradeQuestionCommand(
        BigDecimal score,
        String feedback
) {}
