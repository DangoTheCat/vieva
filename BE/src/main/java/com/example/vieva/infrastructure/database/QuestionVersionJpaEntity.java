package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Phiên bản câu hỏi; sửa câu đã APPROVED sẽ sinh DRAFT mới; bản cũ giữ tham chiếu lịch sử.
 */
@Entity
@Table(
    name = "question_versions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_question_version", columnNames = {"question_id", "version_number"})
    },
    indexes = {
        @Index(name = "idx_qversions_lookup", columnList = "question_id, approval_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionVersionJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "question_version_id", updatable = false, nullable = false)
    private UUID questionVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionJpaEntity question;

    @Builder.Default
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "question_content", columnDefinition = "TEXT", nullable = false)
    private String questionContent;

    @Column(name = "reference_answer", columnDefinition = "TEXT", nullable = false)
    private String referenceAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_level", nullable = false, length = 20)
    private BloomLevel bloomLevel;

    @Column(name = "bloom_confirmed", nullable = false)
    private boolean bloomConfirmed;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_mode", nullable = false, length = 20)
    private QuestionGenerationMode generationMode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "approval_status", nullable = false, length = 20)
    private QuestionApprovalStatus approvalStatus = QuestionApprovalStatus.DRAFT;

    @Column(name = "parent_version_id")
    private UUID parentVersionId;

    @Column(name = "generation_request_id")
    private UUID generationRequestId;

    @Column(name = "regeneration_count", nullable = false)
    private int regenerationCount;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic locking — prevents lost updates when two reviewers edit/approve/reject
     * the same version concurrently (BR-08).
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return questionVersionId;
    }

    @Override
    public boolean isNew() {
        return isNew || createdAt == null;
    }

    @PrePersist
    @PreUpdate
    void ensureUpdatedAt() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
