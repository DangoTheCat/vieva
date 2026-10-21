package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttempt {
    private UUID id;
    private UUID studentId;
    private UUID subjectId;
    private AttemptStatus status;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
    private BigDecimal totalScore;
    private GradeLevel gradeLevel;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExamAttempt start(UUID studentId, UUID subjectId, int totalQuestions) {
        Instant now = Instant.now();
        return ExamAttempt.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .subjectId(subjectId)
                .status(AttemptStatus.IN_PROGRESS)
                .currentQuestionIndex(0)
                .totalQuestions(totalQuestions)
                .totalScore(BigDecimal.ZERO)
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void ensureInProgress() {
        if (status != AttemptStatus.IN_PROGRESS) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Exam attempt is not in progress (current status: " + status + ")");
        }
    }

    public void nextQuestion() {
        ensureInProgress();
        if (currentQuestionIndex + 1 < totalQuestions) {
            this.currentQuestionIndex++;
            this.updatedAt = Instant.now();
        }
    }

    public boolean hasMoreQuestions() {
        return currentQuestionIndex + 1 < totalQuestions;
    }

    public void complete() {
        ensureInProgress();
        this.status = AttemptStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void recordScore(BigDecimal score, GradeLevel grade) {
        this.totalScore = score;
        this.gradeLevel = grade;
        this.updatedAt = Instant.now();
    }

    public void timeout() {
        ensureInProgress();
        this.status = AttemptStatus.TIMED_OUT;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void cancel() {
        ensureInProgress();
        this.status = AttemptStatus.CANCELLED;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }
}
