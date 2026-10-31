package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExamScore {
    private UUID id;
    private UUID attemptId;
    private BigDecimal totalScore;
    private GradeLevel grade;
    private String detailedFeedback;
    private ScoringMode scoringMode;
    private BigDecimal aiScore;
    private String aiFeedback;
    private BigDecimal teacherScore;
    private String teacherFeedback;
    private UUID teacherId;
    private BigDecimal overrideTotalScore;
    private UUID overrideBy;
    private Instant overrideAt;
    private String overrideReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExamScore create(UUID attemptId, BigDecimal totalScore, GradeLevel grade,
                                   String detailedFeedback, ScoringMode scoringMode) {
        Instant now = Instant.now();
        return ExamScore.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .totalScore(totalScore)
                .grade(grade)
                .detailedFeedback(detailedFeedback)
                .scoringMode(scoringMode)
                .aiScore(totalScore)
                .aiFeedback(detailedFeedback)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public BigDecimal getEffectiveTotalScore() {
        if (overrideTotalScore != null) {
            return overrideTotalScore;
        }
        return totalScore != null ? totalScore : BigDecimal.ZERO;
    }

    public void overrideTotal(BigDecimal overrideScore, UUID teacherId) {
        overrideTotal(overrideScore, teacherId, null);
    }

    public void overrideTotal(BigDecimal overrideScore, UUID teacherId, String reason) {
        if (overrideScore != null && (overrideScore.compareTo(BigDecimal.ZERO) < 0 || overrideScore.compareTo(BigDecimal.TEN) > 0)) {
            throw new IllegalArgumentException("Score must be between 0 and 10");
        }
        this.overrideTotalScore = overrideScore;
        this.overrideBy = teacherId;
        this.overrideAt = Instant.now();
        this.overrideReason = reason;
        this.updatedAt = Instant.now();
    }

    /** Records the lecturer's grade after a question was re-graded by hand. */
    public void recordTeacherGrade(BigDecimal score, GradeLevel grade, String feedback, UUID teacherId) {
        this.totalScore = score;
        this.grade = grade;
        this.teacherScore = score;
        this.teacherFeedback = feedback;
        this.teacherId = teacherId;
        this.updatedAt = Instant.now();
    }

    /** Re-bands the grade after the total changed (e.g. an override). */
    public void applyGrade(GradeLevel grade) {
        this.grade = grade;
        this.updatedAt = Instant.now();
    }

    public void clearOverride() {
        this.overrideTotalScore = null;
        this.overrideBy = null;
        this.overrideAt = null;
        this.overrideReason = null;
        this.updatedAt = Instant.now();
    }
}
