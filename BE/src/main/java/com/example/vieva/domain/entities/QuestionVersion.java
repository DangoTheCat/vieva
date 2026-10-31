package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import lombok.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Phiên bản câu hỏi. DRAFT/REJECTED → UNDER_REVIEW → APPROVED | REJECTED; APPROVED → SUPERSEDED when a
 * newer version is approved. Editing an APPROVED version goes through {@link #copyAsDraft}
 * (Copy-on-Write) so the approved content stays untouched for reference.
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVersion {
    private UUID questionVersionId;
    private UUID questionId;
    private Integer versionNumber;
    private String questionContent;
    private String referenceAnswer;
    private BloomLevel bloomLevel;
    private boolean bloomConfirmed;
    private QuestionGenerationMode generationMode;
    private QuestionStatus approvalStatus;
    private UUID parentVersionId;
    private UUID generationRequestId;
    private int regenerationCount;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private String rejectionReason;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    /** Optimistic-lock token (BR-08). */
    private Long version;

    public static QuestionVersion newDraft(UUID questionId, int versionNumber, String content, String answer,
                                           BloomLevel bloomLevel, QuestionGenerationMode mode, UUID createdBy) {
        Instant now = Instant.now();
        return QuestionVersion.builder()
                .questionVersionId(UUID.randomUUID())
                .questionId(questionId)
                .versionNumber(versionNumber)
                .questionContent(content)
                .referenceAnswer(answer)
                .bloomLevel(bloomLevel)
                // A human author picked the level; only AI suggestions need explicit confirmation (BR-01).
                .bloomConfirmed(mode != QuestionGenerationMode.AI_RAG)
                .generationMode(mode)
                .approvalStatus(QuestionStatus.DRAFT)
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isDraft() {
        return approvalStatus == QuestionStatus.DRAFT;
    }

    public boolean isAiGenerated() {
        return generationMode == QuestionGenerationMode.AI_RAG;
    }

    public void ensureDraft() {
        if (!isDraft() && approvalStatus != QuestionStatus.REJECTED) {
            throw new DomainException(DomainErrorCode.VERSION_NOT_DRAFT,
                    "Only DRAFT versions can be changed (current: " + approvalStatus + ")");
        }
    }

    public void submitForReview() {
        ensureDraft();
        approvalStatus = QuestionStatus.UNDER_REVIEW;
        rejectionReason = null;
        touch();
    }

    public void ensureUnderReview() {
        if (approvalStatus != QuestionStatus.UNDER_REVIEW) {
            throw new DomainException(DomainErrorCode.VERSION_NOT_UNDER_REVIEW);
        }
    }

    /** Rejects stale writes when the client sends the lock token it last saw (BR-08). */
    public void ensureVersionMatches(Long expectedVersion) {
        if (expectedVersion != null && !Objects.equals(expectedVersion, version)) {
            throw new DomainException(DomainErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    /**
     * Edits draft content. Changing the Bloom level or confirming it explicitly marks it as
     * reviewed by the lecturer; any other edit keeps the current confirmation flag.
     */
    public void editContent(String content, String answer, BloomLevel level, Boolean confirmBloom) {
        ensureDraft();
        if (content != null) {
            this.questionContent = content.trim();
        }
        if (answer != null) {
            this.referenceAnswer = answer.trim();
        }
        if (level != null && level != this.bloomLevel) {
            this.bloomLevel = level;
            this.bloomConfirmed = true;
        }
        if (confirmBloom != null) {
            this.bloomConfirmed = confirmBloom;
        }
        touch();
    }

    public void confirmBloom(BloomLevel level) {
        ensureDraft();
        if (level != null) {
            this.bloomLevel = level;
        }
        if (this.bloomLevel == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST, "Bloom level is required");
        }
        this.bloomConfirmed = true;
        touch();
    }

    /** Replaces an AI draft in place with regenerated content (UC1.2 – single question). */
    public void replaceWithRegenerated(String content, String answer, BloomLevel level) {
        ensureDraft();
        if (!isAiGenerated()) {
            throw new DomainException(DomainErrorCode.REGENERATION_NOT_SUPPORTED);
        }
        this.questionContent = content;
        this.referenceAnswer = answer;
        this.bloomLevel = level;
        this.bloomConfirmed = false;
        this.regenerationCount = regenerationCount + 1;
        touch();
    }

    public void approve(UUID reviewer) {
        if (approvalStatus != QuestionStatus.UNDER_REVIEW && approvalStatus != QuestionStatus.DRAFT) {
            throw new DomainException(DomainErrorCode.VERSION_NOT_UNDER_REVIEW);
        }
        this.approvalStatus = QuestionStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        this.rejectionReason = null;
        touch();
    }

    public void reject(UUID reviewer, String reason) {
        if (approvalStatus != QuestionStatus.UNDER_REVIEW && approvalStatus != QuestionStatus.DRAFT) {
            throw new DomainException(DomainErrorCode.VERSION_NOT_UNDER_REVIEW);
        }
        if (reason == null || reason.isBlank()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST, "Rejection reason is required");
        }
        this.approvalStatus = QuestionStatus.REJECTED;
        this.rejectionReason = reason.trim();
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        touch();
    }

    public void supersede() {
        if (approvalStatus != QuestionStatus.APPROVED) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST, "Only APPROVED versions can be superseded");
        }
        this.approvalStatus = QuestionStatus.SUPERSEDED;
        touch();
    }

    /**
     * Copy-on-Write: new DRAFT with the same content, pointing to this version as parent.
     * The Bloom level was already confirmed when this version was approved.
     */
    public QuestionVersion copyAsDraft(int newVersionNumber, UUID author) {
        if (approvalStatus != QuestionStatus.APPROVED) {
            throw new DomainException(DomainErrorCode.NO_APPROVED_VERSION,
                    "Only the APPROVED version can be copied into a new draft");
        }
        Instant now = Instant.now();
        return QuestionVersion.builder()
                .questionVersionId(UUID.randomUUID())
                .questionId(questionId)
                .versionNumber(newVersionNumber)
                .questionContent(questionContent)
                .referenceAnswer(referenceAnswer)
                .bloomLevel(bloomLevel)
                .bloomConfirmed(true)
                .generationMode(generationMode)
                .approvalStatus(QuestionStatus.DRAFT)
                .parentVersionId(questionVersionId)
                .generationRequestId(generationRequestId)
                .createdBy(author)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** Bumps the modification timestamp so the optimistic lock also covers rubric/source edits. */
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void linkGenerationRequest(UUID generationRequestId) {
        this.generationRequestId = generationRequestId;
    }
}
