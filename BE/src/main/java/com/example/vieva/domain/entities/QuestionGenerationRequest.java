package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One RAG generation run (UC1.2). Keeps the chosen documents and the retrieved chunks so a
 * single draft can later be regenerated from the same context, and bounds the retries.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerationRequest {
    private UUID generationRequestId;
    private UUID subjectId;
    private UUID topicId;
    private UUID requestedBy;
    @Builder.Default
    private List<UUID> documentIds = new ArrayList<>();
    @Builder.Default
    private Map<BloomLevel, Integer> bloomDistribution = new EnumMap<>(BloomLevel.class);
    private int totalQuestions;
    private String lecturerNote;
    @Builder.Default
    private List<UUID> retrievedChunkIds = new ArrayList<>();
    private QuestionGenerationStatus status;
    private int generatedCount;
    private int rejectedCount;
    private int attemptCount;
    @Builder.Default
    private List<String> issues = new ArrayList<>();
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public static QuestionGenerationRequest start(UUID subjectId, UUID topicId, UUID requestedBy,
                                                  List<UUID> documentIds, BloomDistribution distribution,
                                                  String lecturerNote) {
        Instant now = Instant.now();
        return QuestionGenerationRequest.builder()
                .generationRequestId(UUID.randomUUID())
                .subjectId(subjectId)
                .topicId(topicId)
                .requestedBy(requestedBy)
                .documentIds(new ArrayList<>(documentIds))
                .bloomDistribution(new EnumMap<>(distribution.asMap()))
                .totalQuestions(distribution.total())
                .lecturerNote(lecturerNote)
                .status(QuestionGenerationStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public BloomDistribution distribution() {
        return BloomDistribution.restore(bloomDistribution);
    }

    public void recordRetrievedContext(List<UUID> chunkIds) {
        this.retrievedChunkIds = new ArrayList<>(chunkIds);
        this.updatedAt = Instant.now();
    }

    /** Opens a new LLM attempt; fails once the configured budget is spent. */
    public void beginAttempt(int maxAttempts) {
        if (attemptCount >= maxAttempts) {
            throw new AppException(ErrorCode.RETRY_LIMIT_EXCEEDED,
                    "Generation attempt limit reached (" + maxAttempts + ")");
        }
        this.attemptCount++;
        this.updatedAt = Instant.now();
    }

    public void ensureRetryable() {
        if (status != QuestionGenerationStatus.FAILED && status != QuestionGenerationStatus.PARTIAL) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Only FAILED or PARTIAL generation requests can be retried (current: " + status + ")");
        }
    }

    /** Records the outcome after the attempts of one run: accepted vs rejected candidates. */
    public void recordOutcome(int accepted, int rejected, List<String> newIssues, String error) {
        this.generatedCount += accepted;
        this.rejectedCount += rejected;
        if (newIssues != null) {
            this.issues.addAll(newIssues);
        }
        this.errorMessage = error;
        if (generatedCount >= totalQuestions) {
            this.status = QuestionGenerationStatus.COMPLETED;
        } else if (generatedCount > 0) {
            this.status = QuestionGenerationStatus.PARTIAL;
        } else {
            this.status = QuestionGenerationStatus.FAILED;
        }
        this.updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        recordOutcome(0, 0, List.of(), error);
    }
}
