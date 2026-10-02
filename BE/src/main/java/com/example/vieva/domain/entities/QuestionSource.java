package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Minh chứng xuất xứ ngữ cảnh sinh câu hỏi của AI; câu AI APPROVED bắt buộc có >= 1 source.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSource {
    private UUID questionSourceId;
    private UUID questionVersionId;
    private UUID chunkId;
    private String documentName;
    private String citationQuote;
    private BigDecimal similarityScore;
    private Instant createdAt;

    /**
     * Verifies that the citationQuote actually exists as a substring inside the source chunk content.
     * Whitespace is normalized to prevent formatting/indentation mismatches.
     */
    public void validateCitationGrounding(String chunkContent) {
        if (chunkContent == null || this.citationQuote == null || this.citationQuote.isBlank()) {
            throw new IllegalArgumentException("Citation quote or source chunk content is empty");
        }
        String normalizedChunk = chunkContent.replaceAll("\\s+", " ").trim().toLowerCase();
        String normalizedQuote = this.citationQuote.replaceAll("\\s+", " ").trim().toLowerCase();
        if (!normalizedChunk.contains(normalizedQuote)) {
            throw new IllegalArgumentException("Citation quote does not match source document chunk content");
        }
    }

    /**
     * Enforces the rule documented on this class: an AI-generated (RAG) question may only be
     * APPROVED when it has at least one citation source. Call this before marking a version APPROVED.
     *
     * @throws IllegalArgumentException when an AI_RAG question is approved without any source
     */
    public static void validateRequiredForAiApproved(QuestionGenerationMode generationMode,
                                                     QuestionApprovalStatus approvalStatus,
                                                     Collection<QuestionSource> sources) {
        if (generationMode == QuestionGenerationMode.AI_RAG
                && approvalStatus == QuestionApprovalStatus.APPROVED
                && (sources == null || sources.isEmpty())) {
            throw new IllegalArgumentException(
                    "An AI_RAG question must have at least one source before it can be APPROVED");
        }
    }
}
