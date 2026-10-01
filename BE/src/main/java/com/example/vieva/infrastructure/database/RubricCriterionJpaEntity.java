package com.example.vieva.infrastructure.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Chi tiết các tiêu chí chấm điểm; tổng max_points của các tiêu chí phải bằng total_points của Rubric.
 */
@Entity
@Table(
    name = "rubric_criteria",
    indexes = {
        @Index(name = "idx_criteria_rubric", columnList = "rubric_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricCriterionJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "criterion_id", updatable = false, nullable = false)
    private UUID criterionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private RubricJpaEntity rubric;

    @Column(name = "criterion_name", nullable = false, length = 255)
    private String criterionName;

    @Column(name = "max_points", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPoints;

    @Column(name = "achievement_descriptors", columnDefinition = "TEXT", nullable = false)
    private String achievementDescriptors;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return criterionId;
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
