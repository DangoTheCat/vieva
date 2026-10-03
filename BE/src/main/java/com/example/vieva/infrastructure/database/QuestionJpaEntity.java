package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.QuestionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Bản ghi gốc câu hỏi; ARCHIVED để ngừng cấp cho bài thi mới nhưng giữ vĩnh viễn.
 */
@Entity
@Table(
    name = "questions",
    indexes = {
        @Index(name = "idx_questions_subject_status", columnList = "subject_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "question_id", updatable = false, nullable = false)
    private UUID questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectJpaEntity subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private TopicJpaEntity topic;

    @Column(name = "question_code", nullable = false, unique = true, length = 50)
    private String questionCode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private QuestionStatus status = QuestionStatus.ACTIVE;

    /** Plain FK column: the version table already references questions, so no bidirectional mapping. */
    @Column(name = "current_approved_version_id")
    private UUID currentApprovedVersionId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking — archive and approve race on the same question row. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return questionId;
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
