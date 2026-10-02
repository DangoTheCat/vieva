package com.example.vieva.domain.entities;

import java.math.BigDecimal;

/**
 * One achievement level of a rubric criterion, e.g. {"Đạt một phần", "Nêu được 2/4 ý", 2.5}.
 */
public record PerformanceLevel(String label, String description, BigDecimal score) {
}
