package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionStatus;
import lombok.Builder;

import java.util.UUID;

/**
 * UC1.4 filters. Bloom level and keyword apply to the current approved version.
 *
 * @param sortBy        one of createdAt, updatedAt, questionCode (anything else falls back to updatedAt)
 * @param sortAscending false = newest first
 */
@Builder
public record QuestionBankSearchCriteria(
        UUID subjectId,
        UUID topicId,
        BloomLevel bloomLevel,
        QuestionStatus status,
        String keyword,
        int page,
        int size,
        String sortBy,
        boolean sortAscending
) {
}
