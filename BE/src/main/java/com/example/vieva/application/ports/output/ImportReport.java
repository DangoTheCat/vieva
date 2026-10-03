package com.example.vieva.application.ports.output;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * UC1.6 result. In dry-run mode nothing is written and {@code createdQuestionIds} is empty.
 */
@Builder
public record ImportReport(
        boolean dryRun,
        int totalRows,
        int totalQuestions,
        int validQuestions,
        int invalidQuestions,
        int createdQuestions,
        List<RowError> errors,
        List<UUID> createdQuestionIds
) {
    /**
     * @param column header name, {@code null} when the error concerns the whole row/question
     */
    public record RowError(int row, String column, String questionRef, String message) {
    }
}
