package com.example.vieva.application.ports.input.grading;

import java.math.BigDecimal;
import java.util.UUID;

public record GradingCourseView(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        int examCount,
        long submittedCount,
        long pendingCount,
        long totalParticipants,
        BigDecimal averageScore
) {
    public GradingCourseView(UUID subjectId, String subjectCode, String subjectName, int examCount) {
        this(subjectId, subjectCode, subjectName, examCount, 0, 0, 0, BigDecimal.ZERO);
    }
}
