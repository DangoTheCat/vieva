package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.QuestionGenerationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted RAG run: chosen documents, Bloom distribution, retrieved chunks and outcome.
 */
@Entity
@Table(
    name = "question_generation_requests",
    indexes = {
        @Index(name = "idx_qgen_subject_created", columnList = "subject_id, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionGenerationRequestJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "generation_request_id", updatable = false, nullable = false)
    private UUID generationRequestId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "topic_id")
    private UUID topicId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_ids", columnDefinition = "jsonb", nullable = false)
    private String documentIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bloom_distribution", columnDefinition = "jsonb", nullable = false)
    private String bloomDistribution;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "lecturer_note", columnDefinition = "TEXT")
    private String lecturerNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retrieved_chunk_ids", columnDefinition = "jsonb")
    private String retrievedChunkIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuestionGenerationStatus status;

    @Column(name = "generated_count", nullable = false)
    private int generatedCount;

    @Column(name = "rejected_count", nullable = false)
    private int rejectedCount;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issues_json", columnDefinition = "jsonb")
    private String issuesJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return generationRequestId;
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
