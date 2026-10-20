package com.example.vieva.application.ports.input.question;

import java.math.BigDecimal;

public record PerformanceLevelInput(String label, String description, BigDecimal score) {
}
