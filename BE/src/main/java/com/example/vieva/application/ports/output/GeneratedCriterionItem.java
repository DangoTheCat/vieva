package com.example.vieva.application.ports.output;

import java.math.BigDecimal;

public record GeneratedCriterionItem(
    String criterionName,
    BigDecimal maxPoints,
    String achievementDescriptors,
    int orderIndex
) {}
