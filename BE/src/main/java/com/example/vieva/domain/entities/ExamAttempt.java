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
    private Instant pausedAt;
    private Integer remainingSeconds;
    private Instant graceDeadline;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExamAttempt init(UUID studentId, UUID subjectId, int totalQuestions) {
        return init(studentId, subjectId, totalQuestions, 900);
    }

    public static ExamAttempt init(UUID studentId, UUID subjectId, int totalQuestions, int remainingSeconds) {
        if (studentId == null || subjectId == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Student ID and Subject ID are required to initialize an exam attempt");
        }
        if (totalQuestions <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Total questions must be greater than zero");
        }
        if (remainingSeconds <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Remaining seconds must be greater than zero");
        }
        Instant now = Instant.now();
        return ExamAttempt.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .subjectId(subjectId)
                .status(AttemptStatus.READY)
                .currentQuestionIndex(0)
                .totalQuestions(totalQuestions)
                .remainingSeconds(remainingSeconds)
                .totalScore(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Deprecated
    public static ExamAttempt start(UUID studentId, UUID subjectId, int totalQuestions) {
        ExamAttempt attempt = init(studentId, subjectId, totalQuestions);
        attempt.start(true);
        return attempt;
    }

    public void start(boolean deviceReady) {
        if (this.status != AttemptStatus.READY) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot start exam attempt: status is " + this.status + " (expected READY)");
        }
        if (!deviceReady) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot start exam attempt: device check not passed");
        }
        Instant now = Instant.now();
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void ensureInProgress() {
        if (this.status != AttemptStatus.IN_PROGRESS) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Exam attempt is not in progress (current status: " + this.status + ")");
        }
    }

    public void probe() {
        ensureInProgress();
        this.updatedAt = Instant.now();
    }

    public void nextQuestion() {
        ensureInProgress();
        if (this.currentQuestionIndex == null) {
            this.currentQuestionIndex = 0;
        }
        if (this.totalQuestions != null && this.currentQuestionIndex + 1 < this.totalQuestions) {
            this.currentQuestionIndex++;
            this.updatedAt = Instant.now();
        }
    }

    public boolean hasMoreQuestions() {
        if (this.currentQuestionIndex == null || this.totalQuestions == null) {
            return false;
        }
        return this.currentQuestionIndex + 1 < this.totalQuestions;
    }

    public void disconnect() {
        disconnect(null);
    }

    public void disconnect(Integer currentRemainingSeconds) {
        if (this.status != AttemptStatus.IN_PROGRESS) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot disconnect: exam attempt is not in progress (current status: " + this.status + ")");
        }
        Instant now = Instant.now();
        this.status = AttemptStatus.RECONNECTING;
        this.pausedAt = now;
        this.graceDeadline = now.plusSeconds(120);
        if (currentRemainingSeconds != null) {
            this.remainingSeconds = Math.max(0, currentRemainingSeconds);
        }
        this.updatedAt = now;
    }

    public void reconnect() {
        if (this.status != AttemptStatus.RECONNECTING) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot reconnect: exam attempt is not reconnecting (current status: " + this.status + ")");
        }
        if (this.graceDeadline != null && Instant.now().isAfter(this.graceDeadline)) {
            timeoutGrace();
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot reconnect: grace period has expired");
        }
        if (this.remainingSeconds == null || this.remainingSeconds <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot reconnect: remaining exam time has expired");
        }
        Instant now = Instant.now();
        this.status = AttemptStatus.IN_PROGRESS;
        this.pausedAt = null;
        this.graceDeadline = null;
        this.updatedAt = now;
    }

    public void timeoutGrace() {
        if (this.status != AttemptStatus.RECONNECTING) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot timeout grace: exam attempt is not reconnecting (current status: " + this.status + ")");
        }
        Instant now = Instant.now();
        this.status = AttemptStatus.INTERRUPTED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void finish() {
        ensureInProgress();
        this.status = AttemptStatus.FINALIZING;
        this.updatedAt = Instant.now();
    }

    public void timeoutSession() {
        ensureInProgress();
        this.remainingSeconds = 0;
        this.status = AttemptStatus.FINALIZING;
        this.updatedAt = Instant.now();
    }

    public void syncSuccess() {
        if (this.status != AttemptStatus.FINALIZING) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot complete exam attempt: status is " + this.status + " (expected FINALIZING)");
        }
        Instant now = Instant.now();
        this.status = AttemptStatus.COMPLETED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void syncError() {
        if (this.status != AttemptStatus.FINALIZING) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot mark exam attempt interrupted: status is " + this.status + " (expected FINALIZING)");
        }
        Instant now = Instant.now();
        this.status = AttemptStatus.INTERRUPTED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void complete() {
        if (this.status == AttemptStatus.IN_PROGRESS) {
            finish();
        }
        syncSuccess();
    }

    public void recordScore(BigDecimal score, GradeLevel grade) {
        this.totalScore = score;
        this.gradeLevel = grade;
        this.updatedAt = Instant.now();
    }

    public void updateRemainingSeconds(int remainingSeconds) {
        ensureInProgress();
        this.remainingSeconds = Math.max(0, remainingSeconds);
        this.updatedAt = Instant.now();
    }

    @Deprecated
    public void timeout() {
        timeoutSession();
    }

    @Deprecated
    public void cancel() {
        Instant now = Instant.now();
        this.status = AttemptStatus.INTERRUPTED;
        this.completedAt = now;
        this.updatedAt = now;
    }
}
