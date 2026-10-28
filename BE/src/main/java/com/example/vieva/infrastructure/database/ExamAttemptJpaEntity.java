package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.GradeLevel;
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
        name = "exam_attempts",
        indexes = {
                @Index(name = "idx_exam_attempts_student_id", columnList = "student_id"),
                @Index(name = "idx_exam_attempts_subject_id", columnList = "subject_id"),
                @Index(name = "idx_exam_attempts_exam_id", columnList = "exam_id"),
                @Index(name = "idx_exam_attempts_status", columnList = "status"),
                @Index(name = "idx_exam_attempts_student_status", columnList = "student_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAttemptJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "exam_id")
    private UUID examId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AttemptStatus status;

    @Column(name = "current_question_index", nullable = false)
    private Integer currentQuestionIndex;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", length = 50)
    private GradeLevel gradeLevel;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "remaining_seconds")
    private Integer remainingSeconds;

    @Column(name = "resumed_at")
    private Instant resumedAt;

    @Column(name = "grace_deadline")
    private Instant graceDeadline;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
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
