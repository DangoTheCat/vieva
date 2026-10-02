package com.example.vieva.infrastructure.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Khung Rubric đánh giá; quan hệ 1:1 với QuestionVersion; APPROVED bắt buộc có Rubric hợp lệ.
 */
@Entity
@Table(name = "rubrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "rubric_id", updatable = false, nullable = false)
    private UUID rubricId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_version_id", nullable = false, unique = true)
    private QuestionVersionJpaEntity questionVersion;

    @Column(name = "rubric_name", nullable = false, length = 255)
    private String rubricName;

    @Column(name = "total_points", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalPoints;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return rubricId;
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
