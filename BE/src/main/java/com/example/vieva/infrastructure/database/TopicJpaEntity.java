package com.example.vieva.infrastructure.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Chủ đề kiến thức thuộc một môn học cụ thể.
 */
@Entity
@Table(
    name = "topics",
    indexes = {
        @Index(name = "idx_topics_subject", columnList = "subject_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "topic_id", updatable = false, nullable = false)
    private UUID topicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectJpaEntity subject;

    @Column(name = "topic_name", nullable = false, length = 255)
    private String topicName;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return topicId;
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
