package com.example.vieva.adapters.presenters;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response bodies of the group-1 (question bank) API.
 */
public final class QuestionBankDtos {

    private QuestionBankDtos() {
    }

    /**
     * @param lockVersion optimistic-lock token; send it back as {@code expectedVersion} on writes
     */
    @Builder(toBuilder = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionVersionDto(
            UUID versionId,
            UUID questionId,
            String questionCode,
            UUID subjectId,
            UUID topicId,
            String topicName,
            Integer versionNumber,
            String content,
            String expectedAnswer,
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
            Long lockVersion,
            RubricDto rubric,
            List<QuestionSourceDto> sources
    ) {
    }

    @Builder
    public record RubricDto(UUID rubricId, String name, String description, BigDecimal totalScore,
                            List<RubricCriterionDto> criteria) {
    }

    @Builder
    public record RubricCriterionDto(UUID criterionId, String name, String description, BigDecimal maxScore,
                                     List<PerformanceLevelDto> levels, Integer orderIndex) {
    }

    @Builder
    public record PerformanceLevelDto(String label, String description, BigDecimal score) {
    }

    @Builder
    public record QuestionSourceDto(UUID sourceId, UUID chunkId, UUID documentId, String documentName,
                                    String citationQuote, BigDecimal similarityScore, Integer order) {
    }

    @Builder
    public record QuestionBankItemDto(UUID questionId, String questionCode, UUID subjectId, UUID topicId,
                                      String topicName, String status, boolean hasPendingDraft,
                                      QuestionVersionDto currentVersion, Instant createdAt, Instant updatedAt) {
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionDetailDto(UUID questionId, String questionCode, UUID subjectId, UUID topicId,
                                    String status, UUID currentApprovedVersionId,
                                    QuestionVersionDto currentVersion, QuestionVersionDto pendingDraft,
                                    List<QuestionVersionDto> history, Instant createdAt, Instant updatedAt) {
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerationRequestDto(UUID generationRequestId, UUID subjectId, UUID topicId,
                                       List<UUID> documentIds, Map<String, Integer> bloomDistribution,
                                       int totalQuestions, String lecturerNote, String status,
                                       int generatedCount, int rejectedCount, int attemptCount,
                                       List<String> issues, String errorMessage, Instant createdAt,
                                       Instant updatedAt, List<QuestionVersionDto> drafts) {
    }

    @Builder
    public record TopicDto(UUID topicId, UUID subjectId, String name, String description, Integer orderIndex) {
    }
}
