package com.example.vieva.application.ports.input.grading;

import java.math.BigDecimal;
import java.util.UUID;

public record CourseGradingStatsView(
        UUID courseId,
        int totalExams,
        long totalParticipants,
        long submittedCount,
        long pendingCount,
        BigDecimal averageScore
) {}
