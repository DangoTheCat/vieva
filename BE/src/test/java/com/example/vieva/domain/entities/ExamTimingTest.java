package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Server-side exam clock, per-answer deadline and interview settings (plan items 1.6, 1.8, 2.8). */
class ExamTimingTest {

    private ExamAttempt runningAttempt(int remainingSeconds, Instant resumedAt) {
        ExamAttempt attempt = ExamAttempt.initForExam(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                3, remainingSeconds, 90, 1);
        attempt.start(true);
        org.springframework.test.util.ReflectionTestUtils.setField(attempt, "resumedAt", resumedAt);
        return attempt;
    }

    @Test
    @DisplayName("remainingAt counts down from resumedAt while IN_PROGRESS")
    void remainingCountsDownFromResume() {
        Instant now = Instant.now();
        ExamAttempt attempt = runningAttempt(900, now.minusSeconds(300));
        assertThat(attempt.remainingAt(now)).isEqualTo(600);
        assertThat(attempt.isTimeExpired(now)).isFalse();
    }

    @Test
    @DisplayName("Reloading the page cannot reset the clock: disconnect freezes the server value")
    void disconnectFreezesServerValue() {
        Instant now = Instant.now();
        ExamAttempt attempt = runningAttempt(900, now.minusSeconds(200));
        attempt.disconnect();
        assertThat(attempt.getRemainingSeconds()).isBetween(699, 700);
        assertThat(attempt.getResumedAt()).isNull();
        // While paused the value does not move
        assertThat(attempt.remainingAt(now.plusSeconds(60))).isEqualTo(attempt.getRemainingSeconds());
    }

    @Test
    @DisplayName("A client value can shorten the clock but never extend it")
    void clientValueNeverExtends() {
        Instant now = Instant.now();
        ExamAttempt attempt = runningAttempt(900, now.minusSeconds(500));
        attempt.disconnect(880);
        assertThat(attempt.getRemainingSeconds()).isBetween(399, 400);

        ExamAttempt other = runningAttempt(900, now.minusSeconds(100));
        other.disconnect(300);
        assertThat(other.getRemainingSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("Reconnect restarts the clock from the frozen value")
    void reconnectResumes() {
        ExamAttempt attempt = runningAttempt(900, Instant.now().minusSeconds(100));
        attempt.disconnect();
        int frozen = attempt.getRemainingSeconds();
        attempt.reconnect();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(attempt.getResumedAt()).isNotNull();
        assertThat(attempt.remainingAt(Instant.now())).isBetween(frozen - 1, frozen);
    }

    @Test
    @DisplayName("Expiry and the submit grace window")
    void expiryAndGrace() {
        Instant now = Instant.now();
        ExamAttempt justExpired = runningAttempt(60, now.minusSeconds(65));
        assertThat(justExpired.isTimeExpired(now)).isTrue();
        assertThat(justExpired.isPastSubmitGrace(now)).isFalse();

        ExamAttempt longExpired = runningAttempt(60, now.minusSeconds(60 + ExamAttempt.SUBMIT_GRACE_SECONDS + 5));
        assertThat(longExpired.isPastSubmitGrace(now)).isTrue();
    }

    @Test
    @DisplayName("finish freezes the remaining time")
    void finishFreezes() {
        ExamAttempt attempt = runningAttempt(900, Instant.now().minusSeconds(100));
        attempt.finish();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FINALIZING);
        assertThat(attempt.getRemainingSeconds()).isBetween(799, 800);
    }

    @Test
    @DisplayName("Attempt keeps the limits frozen from the exam")
    void attemptKeepsFrozenLimits() {
        ExamAttempt attempt = runningAttempt(900, Instant.now());
        assertThat(attempt.effectiveAnswerTimeLimitSeconds()).isEqualTo(90);
        assertThat(attempt.effectiveMaxFollowUps()).isEqualTo(1);

        ExamAttempt legacy = ExamAttempt.init(UUID.randomUUID(), UUID.randomUUID(), 2);
        assertThat(legacy.effectiveAnswerTimeLimitSeconds()).isEqualTo(ExamAttempt.DEFAULT_ANSWER_TIME_LIMIT_SECONDS);
        assertThat(legacy.effectiveMaxFollowUps()).isEqualTo(ExamAttempt.DEFAULT_MAX_FOLLOW_UPS);
    }

    @Test
    @DisplayName("Turn answer is flagged late only after deadline + grace")
    void turnTimedOutFlag() {
        Instant askedAt = Instant.now().minusSeconds(200);
        ExamTurn onTime = ExamTurn.createMainQuestion(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "Q", null);
        org.springframework.test.util.ReflectionTestUtils.setField(onTime, "askedAt", askedAt);
        onTime.submitAnswer("A", null, askedAt.plusSeconds(125), 120, 10);
        assertThat(onTime.isTimedOut()).isFalse();

        ExamTurn late = ExamTurn.createMainQuestion(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "Q", null);
        org.springframework.test.util.ReflectionTestUtils.setField(late, "askedAt", askedAt);
        late.submitAnswer("A", null, askedAt.plusSeconds(131), 120, 10);
        assertThat(late.isTimedOut()).isTrue();
        assertThat(late.answerDeadline(120)).isEqualTo(askedAt.plusSeconds(120));
    }

    @Test
    @DisplayName("markAsked restarts the answer timer only for a pending turn")
    void markAsked() {
        ExamTurn turn = ExamTurn.createMainQuestion(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "Q", null);
        Instant later = Instant.now().plusSeconds(5);
        turn.markAsked(later);
        assertThat(turn.getAskedAt()).isEqualTo(later);
        // A second call (page reload) must not restart the timer
        turn.markAsked(later.plusSeconds(60));
        assertThat(turn.getAskedAt()).isEqualTo(later);
        turn.submitAnswer("A", null);
        assertThatThrownBy(() -> turn.markAsked(Instant.now())).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Exam interview settings are validated and locked once the exam opens")
    void examInterviewSettings() {
        Exam exam = Exam.create(UUID.randomUUID(), UUID.randomUUID(), "Midterm", 900, 90, 3);
        assertThat(exam.getAnswerTimeLimitSeconds()).isEqualTo(90);
        assertThat(exam.getMaxFollowUpsPerQuestion()).isEqualTo(3);

        assertThatThrownBy(() -> exam.applyInterviewSettings(10, null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> exam.applyInterviewSettings(null, 4)).isInstanceOf(DomainException.class);

        exam.applyInterviewSettings(null, 0);
        assertThat(exam.getMaxFollowUpsPerQuestion()).isZero();
        assertThat(exam.getAnswerTimeLimitSeconds()).isEqualTo(90);

        exam.transitionTo(ExamStatus.IN_PROGRESS);
        assertThatThrownBy(() -> exam.applyInterviewSettings(120, null)).isInstanceOf(DomainException.class);

        Exam defaults = Exam.create(UUID.randomUUID(), UUID.randomUUID(), "Final", null);
        assertThat(defaults.getAnswerTimeLimitSeconds()).isEqualTo(Exam.DEFAULT_ANSWER_TIME_LIMIT_SECONDS);
        assertThat(defaults.getMaxFollowUpsPerQuestion()).isEqualTo(Exam.DEFAULT_MAX_FOLLOW_UPS);
    }
}
