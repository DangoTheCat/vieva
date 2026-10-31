package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One RAG generation run (UC1.2). Keeps the chosen documents and the retrieved chunks so a
 * single draft can later be regenerated from the same context, and bounds the retries.
 */
@Getter
@Builder(toBuilder = true)
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

    public List<UUID> getDocumentIds() {
        return documentIds != null ? Collections.unmodifiableList(documentIds) : Collections.emptyList();
    }

    public void replaceDocumentIds(List<UUID> documentIds) {
        this.documentIds = documentIds != null ? new ArrayList<>(documentIds) : new ArrayList<>();
    }

    public Map<BloomLevel, Integer> getBloomDistribution() {
        return bloomDistribution != null ? Collections.unmodifiableMap(bloomDistribution) : Collections.emptyMap();
    }

    public void replaceBloomDistribution(Map<BloomLevel, Integer> bloomDistribution) {
        this.bloomDistribution = bloomDistribution != null ? new EnumMap<>(bloomDistribution) : new EnumMap<>(BloomLevel.class);
    }

    public List<UUID> getRetrievedChunkIds() {
        return retrievedChunkIds != null ? Collections.unmodifiableList(retrievedChunkIds) : Collections.emptyList();
    }

    public void replaceRetrievedChunkIds(List<UUID> retrievedChunkIds) {
        this.retrievedChunkIds = retrievedChunkIds != null ? new ArrayList<>(retrievedChunkIds) : new ArrayList<>();
    }

    public List<String> getIssues() {
        return issues != null ? Collections.unmodifiableList(issues) : Collections.emptyList();
    }

    public void replaceIssues(List<String> issues) {
        this.issues = issues != null ? new ArrayList<>(issues) : new ArrayList<>();
    }

    public static QuestionGenerationRequest start(UUID subjectId, UUID topicId, UUID requestedBy,
                                                  List<UUID> documentIds, BloomDistribution distribution,
                                                  String lecturerNote) {
        Instant now = Instant.now();
        return QuestionGenerationRequest.builder()
                .generationRequestId(UUID.randomUUID())
                .subjectId(subjectId)
                .topicId(topicId)
                .requestedBy(requestedBy)
                .documentIds(documentIds == null ? new ArrayList<>() : new ArrayList<>(documentIds))
                .bloomDistribution(distribution == null ? new EnumMap<>(BloomLevel.class) : new EnumMap<>(distribution.asMap()))
                .totalQuestions(distribution == null ? 0 : distribution.total())
                .lecturerNote(lecturerNote)
                .status(QuestionGenerationStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public BloomDistribution distribution() {
        return BloomDistribution.restore(bloomDistribution);
    }

    public void recordRetrievedChunks(List<UUID> chunkIds, Instant now) {
        this.retrievedChunkIds = chunkIds == null ? new ArrayList<>() : new ArrayList<>(chunkIds);
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void recordRetrievedContext(List<UUID> chunkIds) {
        recordRetrievedChunks(chunkIds, Instant.now());
    }

    public void recordAttempts(int attemptCount) {
        this.attemptCount = Math.max(this.attemptCount, attemptCount);
        this.updatedAt = Instant.now();
    }

    /** Opens a new LLM attempt; fails once the configured budget is spent. */
    public void beginAttempt(int maxAttempts) {
        if (attemptCount >= maxAttempts) {
            throw new DomainException(DomainErrorCode.RETRY_LIMIT_EXCEEDED,
                    "Generation attempt limit reached (" + maxAttempts + ")");
        }
        this.attemptCount++;
        this.updatedAt = Instant.now();
    }

    public void ensureRetryable() {
        if (status != QuestionGenerationStatus.FAILED && status != QuestionGenerationStatus.PARTIAL) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Only FAILED or PARTIAL generation requests can be retried (current: " + status + ")");
        }
    }

    /** Records the outcome after the attempts of one run: accepted vs rejected candidates. */
    public void recordOutcome(int accepted, int rejected, List<String> newIssues, String error) {
        this.generatedCount += Math.max(0, accepted);
        this.rejectedCount += Math.max(0, rejected);
        if (newIssues != null && !newIssues.isEmpty()) {
            List<String> mutableIssues = (this.issues == null)
                    ? new ArrayList<>()
                    : new ArrayList<>(this.issues);
            mutableIssues.addAll(newIssues);
            this.issues = mutableIssues;
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
