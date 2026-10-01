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
    private String citationQuote;
    private BigDecimal similarityScore;
    private Instant createdAt;

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
