package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExamAttemptStateMachineTest {

    private final UUID studentId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    private void assertErrorCode(Runnable action, DomainErrorCode expectedCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(expectedCode);
    }

    @Test
    @DisplayName("init() creates attempt in READY status with 900 remaining seconds")
    void init_createsAttemptInReadyStatus() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);

        assertThat(attempt.getId()).isNotNull();
        assertThat(attempt.getStudentId()).isEqualTo(studentId);
        assertThat(attempt.getSubjectId()).isEqualTo(subjectId);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.READY);
        assertThat(attempt.getTotalQuestions()).isEqualTo(5);
        assertThat(attempt.getCurrentQuestionIndex()).isZero();
        assertThat(attempt.getRemainingSeconds()).isEqualTo(900);
        assertThat(attempt.getTotalScore()).isEqualTo(BigDecimal.ZERO);
        assertThat(attempt.getStartedAt()).isNull();
        assertThat(attempt.getCompletedAt()).isNull();
        assertThat(attempt.getPausedAt()).isNull();
        assertThat(attempt.getGraceDeadline()).isNull();
    }

    @Test
    @DisplayName("start(deviceReady=true) transitions READY -> IN_PROGRESS and stamps startedAt")
    void start_withDeviceReady_transitionsToInProgress() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        Instant before = Instant.now();

        attempt.start(true);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(attempt.getStartedAt()).isNotNull();
        assertThat(attempt.getStartedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("start(deviceReady=false) rejects transition and leaves status as READY")
    void start_withDeviceNotReady_throwsException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);

        assertErrorCode(() -> attempt.start(false), DomainErrorCode.INVALID_REQUEST);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.READY);
        assertThat(attempt.getStartedAt()).isNull();
    }

    @Test
    @DisplayName("start() throws if attempt is not in READY status")
    void start_whenNotReady_throwsException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);

        assertErrorCode(() -> attempt.start(true), DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("disconnect() transitions IN_PROGRESS -> RECONNECTING and sets grace period")
    void disconnect_happyPath_transitionsToReconnecting() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);

        Instant before = Instant.now();
        attempt.disconnect(600);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.RECONNECTING);
        assertThat(attempt.getRemainingSeconds()).isEqualTo(600);
        assertThat(attempt.getPausedAt()).isNotNull();
        assertThat(attempt.getPausedAt()).isAfterOrEqualTo(before);
        assertThat(attempt.getGraceDeadline()).isNotNull();
        assertThat(attempt.getGraceDeadline()).isAfter(attempt.getPausedAt());
    }

    @Test
    @DisplayName("disconnect() throws if attempt is not IN_PROGRESS")
    void disconnect_whenNotRunning_throwsException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);

        assertErrorCode(attempt::disconnect, DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("reconnect() transitions RECONNECTING -> IN_PROGRESS and clears pause state")
    void reconnect_happyPath_transitionsToInProgress() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);
        attempt.disconnect(500);

        attempt.reconnect();

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(attempt.getPausedAt()).isNull();
        assertThat(attempt.getGraceDeadline()).isNull();
        assertThat(attempt.getRemainingSeconds()).isEqualTo(500);
    }

    @Test
    @DisplayName("reconnect() throws if remaining time <= 0")
    void reconnect_whenTimeExpired_throwsException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);
        attempt.disconnect(0);

        assertErrorCode(attempt::reconnect, DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("reconnect() triggers timeoutGrace and marks INTERRUPTED if grace deadline expired")
    void reconnect_whenGracePeriodExpired_throwsAndInterrupts() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);
        attempt.disconnect(400);

        // Simulate expired grace deadline
        attempt.setGraceDeadline(Instant.now().minusSeconds(10));

        assertErrorCode(attempt::reconnect, DomainErrorCode.INVALID_REQUEST);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
        assertThat(attempt.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("timeoutGrace() transitions RECONNECTING -> INTERRUPTED")
    void timeoutGrace_happyPath_transitionsToInterrupted() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);
        attempt.disconnect(300);

        attempt.timeoutGrace();

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
        assertThat(attempt.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("timeoutGrace() throws if attempt is not in RECONNECTING status")
    void timeoutGrace_whenNotInReconnecting_throwsException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);

        assertErrorCode(attempt::timeoutGrace, DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("finish() transitions IN_PROGRESS -> FINALIZING")
    void finish_happyPath_transitionsToFinalizing() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);

        attempt.finish();

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FINALIZING);
    }

    @Test
    @DisplayName("timeoutSession() transitions IN_PROGRESS -> FINALIZING with remainingSeconds=0")
    void timeoutSession_happyPath_transitionsToFinalizing() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);

        attempt.timeoutSession();

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FINALIZING);
        assertThat(attempt.getRemainingSeconds()).isZero();
    }

    @Test
    @DisplayName("finish() / timeoutSession() throw if not IN_PROGRESS")
    void finishAndTimeoutSession_whenNotInProgress_throwException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);

        assertErrorCode(attempt::finish, DomainErrorCode.INVALID_REQUEST);
        assertErrorCode(attempt::timeoutSession, DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("syncSuccess() transitions FINALIZING -> COMPLETED and stamps completedAt")
    void syncSuccess_happyPath_transitionsToCompleted() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);
        attempt.finish();

        Instant before = Instant.now();
        attempt.syncSuccess();

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(attempt.getCompletedAt()).isNotNull();
        assertThat(attempt.getCompletedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("syncSuccess() throws if attempt is not FINALIZING")
    void syncSuccess_whenNotFinalizing_throwsException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);

        assertErrorCode(attempt::syncSuccess, DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("syncError() transitions FINALIZING -> INTERRUPTED and stamps completedAt")
    void syncError_happyPath_transitionsToInterrupted() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);
        attempt.finish();

        Instant before = Instant.now();
        attempt.syncError();

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
        assertThat(attempt.getCompletedAt()).isNotNull();
        assertThat(attempt.getCompletedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("syncError() throws if attempt is not FINALIZING")
    void syncError_whenNotFinalizing_throwsException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);

        assertErrorCode(attempt::syncError, DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("probe() and nextQuestion() operate when IN_PROGRESS")
    void inProgressOperations_workCorrectly() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 2);
        attempt.start(true);

        attempt.probe();
        assertThat(attempt.getCurrentQuestionIndex()).isZero();
        assertThat(attempt.hasMoreQuestions()).isTrue();

        attempt.nextQuestion();
        assertThat(attempt.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(attempt.hasMoreQuestions()).isFalse();

        // Bounded question index
        attempt.nextQuestion();
        assertThat(attempt.getCurrentQuestionIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("init() rejects invalid parameters")
    void init_withInvalidParameters_throwsException() {
        assertErrorCode(() -> ExamAttempt.init(null, subjectId, 5), DomainErrorCode.INVALID_REQUEST);
        assertErrorCode(() -> ExamAttempt.init(studentId, null, 5), DomainErrorCode.INVALID_REQUEST);
        assertErrorCode(() -> ExamAttempt.init(studentId, subjectId, 0), DomainErrorCode.INVALID_REQUEST);
        assertErrorCode(() -> ExamAttempt.init(studentId, subjectId, -1), DomainErrorCode.INVALID_REQUEST);
        assertErrorCode(() -> ExamAttempt.init(studentId, subjectId, 5, 0), DomainErrorCode.INVALID_REQUEST);
        assertErrorCode(() -> ExamAttempt.init(studentId, subjectId, 5, -10), DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("disconnect() without arguments pauses timer and defaults remainingSeconds")
    void disconnect_withoutArguments_pausesTimer() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);

        Instant before = Instant.now();
        attempt.disconnect();

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.RECONNECTING);
        assertThat(attempt.getRemainingSeconds()).isEqualTo(900);
        assertThat(attempt.getPausedAt()).isNotNull();
        assertThat(attempt.getPausedAt()).isAfterOrEqualTo(before);
        assertThat(attempt.getGraceDeadline()).isNotNull();
    }

    @Test
    @DisplayName("reconnect() throws if remainingSeconds is null or negative")
    void reconnect_whenRemainingSecondsNullOrNegative_throwsException() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        attempt.start(true);
        attempt.disconnect();

        attempt.setRemainingSeconds(null);
        assertErrorCode(attempt::reconnect, DomainErrorCode.INVALID_REQUEST);

        attempt.setRemainingSeconds(-5);
        assertErrorCode(attempt::reconnect, DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("updateRemainingSeconds() operates when IN_PROGRESS and rejects when in other statuses")
    void updateRemainingSeconds_validatesStatus() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 3);
        assertErrorCode(() -> attempt.updateRemainingSeconds(500), DomainErrorCode.INVALID_REQUEST);

        attempt.start(true);
        attempt.updateRemainingSeconds(500);
        assertThat(attempt.getRemainingSeconds()).isEqualTo(500);

        attempt.updateRemainingSeconds(-10);
        assertThat(attempt.getRemainingSeconds()).isZero();

        attempt.finish();
        assertErrorCode(() -> attempt.updateRemainingSeconds(300), DomainErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("nextQuestion() and hasMoreQuestions() handle null fields gracefully")
    void nextQuestion_handlesNullFieldsGracefully() {
        ExamAttempt attempt = ExamAttempt.builder()
                .status(AttemptStatus.IN_PROGRESS)
                .currentQuestionIndex(null)
                .totalQuestions(null)
                .build();

        assertThat(attempt.hasMoreQuestions()).isFalse();
        attempt.nextQuestion();
        assertThat(attempt.getCurrentQuestionIndex()).isZero();
    }
}
