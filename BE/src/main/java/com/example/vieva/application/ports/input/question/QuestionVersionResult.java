package com.example.vieva.application.ports.input.question;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record QuestionVersionResult(
        UUID versionId,
        UUID questionId,
        Integer versionNumber,
        String questionContent,
        String referenceAnswer,
        String bloomLevel,
        String bloomLevelLabel,
        boolean bloomConfirmed,
        String origin,
        String status,
        UUID parentVersionId,
        UUID generationRequestId,
        int regenerationCount,
        String rejectReason,
        UUID createdBy,
        UUID reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt,
        Long lockVersion
) {
}
