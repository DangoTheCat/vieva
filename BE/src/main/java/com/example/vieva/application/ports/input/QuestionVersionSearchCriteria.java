package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import lombok.Builder;

import java.util.UUID;

/**
 * Review queue filters (UC1.3), e.g. status=DRAFT for the drafts awaiting review.
 */
@Builder
public record QuestionVersionSearchCriteria(
        UUID subjectId,
        UUID topicId,
        QuestionApprovalStatus status,
        QuestionGenerationMode origin,
        BloomLevel bloomLevel,
        UUID generationRequestId,
        String keyword,
        int page,
        int size
) {
}
