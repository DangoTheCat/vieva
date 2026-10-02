package com.example.vieva.infrastructure.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Minh chứng xuất xứ ngữ cảnh sinh câu hỏi của AI; câu AI APPROVED bắt buộc có >= 1 source.
 */
@Entity
@Table(
    name = "question_sources",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_question_version_chunk", columnNames = {"question_version_id", "chunk_id"})
    },
    indexes = {
        @Index(name = "idx_question_sources_version", columnList = "question_version_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionSourceJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "question_source_id", updatable = false, nullable = false)
    private UUID questionSourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_version_id", nullable = false)
    private QuestionVersionJpaEntity questionVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id")
    private DocumentChunkJpaEntity chunk;

    @Column(name = "document_id")
    private UUID documentId;

    @Builder.Default
    @Column(name = "source_order", nullable = false)
    private Integer sourceOrder = 1;

    @Column(name = "document_name", length = 255)
    private String documentName;

    @Column(name = "citation_quote", columnDefinition = "TEXT", nullable = false)
    private String citationQuote;

    @Column(name = "similarity_score", precision = 4, scale = 3)
    private BigDecimal similarityScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return questionSourceId;
    }

    @Override
    public boolean isNew() {
        return isNew || createdAt == null;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
