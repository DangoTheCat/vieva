package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.TurnStatus;
import com.example.vieva.domain.entities.TurnType;
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
        name = "exam_turns",
        indexes = {
                @Index(name = "idx_exam_turns_attempt_id", columnList = "attempt_id"),
                @Index(name = "idx_exam_turns_status", columnList = "status"),
                @Index(name = "idx_exam_turns_attempt_order", columnList = "attempt_id, turn_order"),
                @Index(name = "idx_exam_turns_parent_id", columnList = "parent_turn_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTurnJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "question_version_id")
    private UUID questionVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "turn_type", nullable = false, length = 50)
    private TurnType turnType;

    @Column(name = "parent_turn_id")
    private UUID parentTurnId;

    @Column(name = "turn_order", nullable = false)
    private Integer turnOrder;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "tts_audio_url", columnDefinition = "TEXT")
    private String ttsAudioUrl;

    @Column(name = "student_audio_url", columnDefinition = "TEXT")
    private String studentAudioUrl;

    @Column(name = "transcript_text", columnDefinition = "TEXT")
    private String transcriptText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TurnStatus status;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "rubric_evaluations", columnDefinition = "jsonb")
    private String rubricEvaluations;

    @Column(name = "teacher_score", precision = 5, scale = 2)
    private BigDecimal teacherScore;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "teacher_graded_at")
    private Instant teacherGradedAt;

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
