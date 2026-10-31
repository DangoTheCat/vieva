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
    private UUID examId;
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
    /**
     * Moment the exam clock last started running (start or reconnect). While IN_PROGRESS the
     * remaining time is {@code remainingSeconds - (now - resumedAt)}; in every other state
     * {@code remainingSeconds} is the frozen value. The server is the only source of truth.
     */
    private Instant resumedAt;
    /** Per-answer time limit frozen from the exam when the attempt is created. */
    private Integer answerTimeLimitSeconds;
    /** Maximum adaptive follow-up questions per main question, frozen from the exam. */
    private Integer maxFollowUpsPerQuestion;
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

    public static final int DEFAULT_ANSWER_TIME_LIMIT_SECONDS = 120;
    public static final int DEFAULT_MAX_FOLLOW_UPS = 2;
    /** Extra seconds accepted after a deadline to absorb network and upload latency. */
    public static final int SUBMIT_GRACE_SECONDS = 10;

    public static ExamAttempt initForExam(UUID examId, UUID studentId, UUID subjectId, int totalQuestions, int remainingSeconds) {
        return initForExam(examId, studentId, subjectId, totalQuestions, remainingSeconds,
                DEFAULT_ANSWER_TIME_LIMIT_SECONDS, DEFAULT_MAX_FOLLOW_UPS);
    }

    public static ExamAttempt initForExam(UUID examId, UUID studentId, UUID subjectId, int totalQuestions,
                                          int remainingSeconds, int answerTimeLimitSeconds, int maxFollowUpsPerQuestion) {
        ExamAttempt attempt = init(studentId, subjectId, totalQuestions, remainingSeconds);
        attempt.examId = examId;
        attempt.answerTimeLimitSeconds = answerTimeLimitSeconds;
        attempt.maxFollowUpsPerQuestion = maxFollowUpsPerQuestion;
        return attempt;
    }

    /** Seconds left on the exam clock at {@code now}, computed on the server. */
    public int remainingAt(Instant now) {
        int frozen = this.remainingSeconds != null ? this.remainingSeconds : 0;
        if (this.status != AttemptStatus.IN_PROGRESS) {
            return Math.max(0, frozen);
        }
        Instant anchor = this.resumedAt != null ? this.resumedAt : this.startedAt;
        if (anchor == null) {
            return Math.max(0, frozen);
        }
        long elapsed = Math.max(0, java.time.Duration.between(anchor, now).getSeconds());
        return (int) Math.max(0, frozen - elapsed);
    }

    /** True once the clock has run out, ignoring the submit grace window. */
    public boolean isTimeExpired(Instant now) {
        return this.status == AttemptStatus.IN_PROGRESS && remainingAt(now) <= 0;
    }

    /** True when the clock ran out more than {@link #SUBMIT_GRACE_SECONDS} ago. */
    public boolean isPastSubmitGrace(Instant now) {
        if (this.status != AttemptStatus.IN_PROGRESS) {
            return false;
        }
        return isTimeExpired(now.minusSeconds(SUBMIT_GRACE_SECONDS));
    }

    public int effectiveAnswerTimeLimitSeconds() {
        return this.answerTimeLimitSeconds != null && this.answerTimeLimitSeconds > 0
                ? this.answerTimeLimitSeconds
                : DEFAULT_ANSWER_TIME_LIMIT_SECONDS;
    }

    public int effectiveMaxFollowUps() {
        return this.maxFollowUpsPerQuestion != null && this.maxFollowUpsPerQuestion >= 0
                ? this.maxFollowUpsPerQuestion
                : DEFAULT_MAX_FOLLOW_UPS;
    }

    /** Freezes the running clock into {@code remainingSeconds}. */
    private void freezeClock(Instant now) {
        this.remainingSeconds = remainingAt(now);
        this.resumedAt = null;
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
        this.resumedAt = now;
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

    /**
     * Pauses the clock. The remaining time is computed on the server; a client value can only
     * shorten it (never extend it), so reloading the page cannot reset the timer.
     */
    public void disconnect(Integer clientRemainingSeconds) {
        if (this.status != AttemptStatus.IN_PROGRESS) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Cannot disconnect: exam attempt is not in progress (current status: " + this.status + ")");
        }
        Instant now = Instant.now();
        freezeClock(now);
        if (clientRemainingSeconds != null) {
            this.remainingSeconds = Math.max(0, Math.min(this.remainingSeconds, clientRemainingSeconds));
        }
        this.status = AttemptStatus.RECONNECTING;
        this.pausedAt = now;
        this.graceDeadline = now.plusSeconds(120);
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
        this.resumedAt = now;
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
        if (this.status == AttemptStatus.FINALIZING || this.status == AttemptStatus.COMPLETED) {
            return;
        }
        ensureInProgress();
        Instant now = Instant.now();
        freezeClock(now);
        this.status = AttemptStatus.FINALIZING;
        this.updatedAt = now;
    }

    public void timeoutSession() {
        ensureInProgress();
        this.remainingSeconds = 0;
        this.resumedAt = null;
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

    /** Accepts a client clock value only when it is lower than the server's (never extends time). */
    public void updateRemainingSeconds(int remainingSeconds) {
        ensureInProgress();
        Instant now = Instant.now();
        int serverRemaining = remainingAt(now);
        this.remainingSeconds = Math.max(0, Math.min(serverRemaining, remainingSeconds));
        this.resumedAt = now;
        this.updatedAt = now;
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
