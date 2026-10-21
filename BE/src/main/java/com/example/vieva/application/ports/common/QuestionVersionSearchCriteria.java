package com.example.vieva.application.ports.common;

import lombok.Builder;

import java.util.UUID;

/**
 * Review queue filters (UC1.3), e.g. status=DRAFT for the drafts awaiting review.
 */
@Builder
public record QuestionVersionSearchCriteria(
        UUID subjectId,
        UUID topicId,
        String status,
        String origin,
        String bloomLevel,
        UUID generationRequestId,
        String keyword,
        int page,
        int size
) {
}
