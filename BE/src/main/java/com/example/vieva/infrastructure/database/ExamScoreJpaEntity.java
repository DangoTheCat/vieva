package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.GradeLevel;
import com.example.vieva.domain.entities.ScoringMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "exam_scores",
        indexes = {
                @Index(name = "idx_exam_scores_attempt_id", columnList = "attempt_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamScoreJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "attempt_id", nullable = false, unique = true)
    private UUID attemptId;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 50)
    private GradeLevel grade;

    @Column(name = "detailed_feedback", columnDefinition = "TEXT")
    private String detailedFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_mode", nullable = false, length = 50)
    private ScoringMode scoringMode;

    @Column(name = "ai_score", precision = 5, scale = 2)
    private BigDecimal aiScore;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "teacher_score", precision = 5, scale = 2)
    private BigDecimal teacherScore;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "override_total_score", precision = 5, scale = 2)
    private BigDecimal overrideTotalScore;

    @Column(name = "override_by")
    private UUID overrideBy;

    @Column(name = "override_at")
    private Instant overrideAt;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNewEntity = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNewEntity = false;
    }
}
