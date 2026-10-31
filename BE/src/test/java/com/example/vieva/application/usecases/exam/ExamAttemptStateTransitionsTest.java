package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.common.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.domain.entities.*;
import com.example.vieva.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamAttemptStateTransitionsTest {

    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private ExamTurnRepository examTurnRepository;
    @Mock
    private AttemptGradingService attemptGradingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionVersionRepository questionVersionRepository;
    @Mock
    private com.example.vieva.application.ports.output.ExamAttemptQuestionSnapshotRepository snapshotRepository;

    private final UUID studentId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("StartExamUseCase: Initialization to READY state")
    class StartExamTests {
        @Test
        @DisplayName("startExam initializes attempt in READY state with questions prepared")
        void startExamCreatesAttemptInReadyState() {
            User student = User.builder().userId(studentId).status(UserStatus.ACTIVE).build();
            Subject subject = Subject.builder().subjectId(subjectId).status(SubjectStatus.ACTIVE).build();

            UUID qId = UUID.randomUUID();
            UUID qvId = UUID.randomUUID();
            Question question = Question.builder().questionId(qId).currentApprovedVersionId(qvId).build();
            QuestionVersion qv = QuestionVersion.builder().questionVersionId(qvId).questionContent("What is polymorphism?").build();

            when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
            when(questionRepository.searchBank(any(QuestionBankSearchCriteria.class)))
                    .thenReturn(PageResult.<Question>builder().content(List.of(question)).totalElements(1L).build());
            when(questionVersionRepository.findById(qvId)).thenReturn(Optional.of(qv));
            when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

            StartExamUseCaseImpl startExamUseCase = new StartExamUseCaseImpl(
                    userRepository, subjectRepository, questionRepository,
                    questionVersionRepository, examAttemptRepository, examTurnRepository,
                    snapshotRepository
            );

            ExamAttemptView view = startExamUseCase.startExam(studentId, subjectId);

            assertThat(view).isNotNull();
            assertThat(view.status()).isEqualTo("READY");
            verify(examAttemptRepository).save(argThat(a -> a.getStatus() == AttemptStatus.READY));
        }
    }

    @Nested
    @DisplayName("StartExamSessionUseCase: Transition READY -> IN_PROGRESS")
    class StartSessionTests {
        @Test
        @DisplayName("startSession transitions attempt from READY to IN_PROGRESS when device is ready")
        void startSessionSuccess() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            ExamTurn turn = ExamTurn.createMainQuestion(attemptId, UUID.randomUUID(), UUID.randomUUID(), 1, "Q1", null);
            when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

            StartExamSessionUseCaseImpl useCase = new StartExamSessionUseCaseImpl(examAttemptRepository, examTurnRepository);

            ExamAttemptView view = useCase.startSession(attemptId, studentId, true);

            assertThat(view.status()).isEqualTo("IN_PROGRESS");
            assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
            assertThat(attempt.getStartedAt()).isNotNull();
            verify(examAttemptRepository).save(attempt);
        }

        @Test
        @DisplayName("startSession fails when device check is not passed")
        void startSessionFailsWhenDeviceNotReady() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            StartExamSessionUseCaseImpl useCase = new StartExamSessionUseCaseImpl(examAttemptRepository, examTurnRepository);

            assertThatThrownBy(() -> useCase.startSession(attemptId, studentId, false))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("device check not passed");
        }

        @Test
        @DisplayName("startSession fails when student does not own attempt")
        void startSessionUnauthorized() {
            ExamAttempt attempt = ExamAttempt.init(UUID.randomUUID(), subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            StartExamSessionUseCaseImpl useCase = new StartExamSessionUseCaseImpl(examAttemptRepository, examTurnRepository);

            assertThatThrownBy(() -> useCase.startSession(attemptId, studentId, true))
                    .isInstanceOf(ApplicationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("startSession fails when attempt is not in READY state")
        void startSessionFailsIfNotReady() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            attempt.start(true); // Now IN_PROGRESS
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            StartExamSessionUseCaseImpl useCase = new StartExamSessionUseCaseImpl(examAttemptRepository, examTurnRepository);

            assertThatThrownBy(() -> useCase.startSession(attemptId, studentId, true))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("expected READY");
        }
    }

    @Nested
    @DisplayName("ManageExamConnectionUseCase: Disconnect, Reconnect, Finish")
    class ConnectionManagementTests {

        private ManageExamConnectionUseCaseImpl useCase;

        @BeforeEach
        void initUseCase() {
            useCase = new ManageExamConnectionUseCaseImpl(examAttemptRepository, examTurnRepository,
                    attemptId -> { });
        }

        @Test
        @DisplayName("disconnect transitions IN_PROGRESS -> RECONNECTING and sets pausedAt")
        void disconnectSuccess() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            attempt.start(true);
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            ExamAttemptView view = useCase.disconnect(attemptId, studentId);

            assertThat(view.status()).isEqualTo("RECONNECTING");
            assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.RECONNECTING);
            assertThat(attempt.getPausedAt()).isNotNull();
            assertThat(attempt.getGraceDeadline()).isNotNull();
            verify(examAttemptRepository).save(attempt);
        }

        @Test
        @DisplayName("disconnect throws if attempt is not IN_PROGRESS")
        void disconnectFailsIfNotInProgress() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId); // READY
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            assertThatThrownBy(() -> useCase.disconnect(attemptId, studentId))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("not in progress");
        }

        @Test
        @DisplayName("reconnect transitions RECONNECTING -> IN_PROGRESS and clears pausedAt")
        void reconnectSuccess() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            attempt.start(true);
            attempt.disconnect();
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            ExamAttemptView view = useCase.reconnect(attemptId, studentId);

            assertThat(view.status()).isEqualTo("IN_PROGRESS");
            assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
            assertThat(attempt.getPausedAt()).isNull();
            assertThat(attempt.getGraceDeadline()).isNull();
            verify(examAttemptRepository, atLeastOnce()).save(attempt);
        }

        @Test
        @DisplayName("reconnect fails if grace period expired and saves attempt as INTERRUPTED")
        void reconnectFailsIfGraceExpired() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            attempt.start(true);
            attempt.disconnect();
            // Force graceDeadline to past
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "graceDeadline", Instant.now().minusSeconds(10));
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            assertThatThrownBy(() -> useCase.reconnect(attemptId, studentId))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("grace period has expired");
            assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
            verify(examAttemptRepository).save(attempt);
        }

        @Test
        @DisplayName("finish transitions IN_PROGRESS -> FINALIZING and triggers async grading")
        void finishSuccess() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 5);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            attempt.start(true);
            when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

            ExamAttemptView view = useCase.finish(attemptId, studentId);

            assertThat(view.status()).isEqualTo("FINALIZING");
            assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FINALIZING);
            verify(examAttemptRepository).save(attempt);
        }
    }

    @Nested
    @DisplayName("AttemptGradingServiceImpl: syncSuccess & syncError")
    class AttemptGradingTests {

        @Mock
        private AttemptGradingGateway attemptGradingGateway;
        @Mock
        private RuleBasedGraderPort ruleBasedGraderPort;
        @Mock
        private RubricRepository rubricRepository;
        @Mock
        private ExamScoreRepository examScoreRepository;

        private AttemptGradingServiceImpl gradingService;

        @BeforeEach
        void initService() {
            gradingService = new AttemptGradingServiceImpl(
                    attemptGradingGateway,
                    ruleBasedGraderPort,
                    rubricRepository,
                    examAttemptRepository,
                    examScoreRepository
            );
        }

        @Test
        @DisplayName("gradeAndCompleteAttempt transitions FINALIZING -> COMPLETED on successful grading")
        void gradingSuccessTransitionsToCompleted() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            attempt.start(true);
            attempt.finish(); // FINALIZING

            AttemptGradingResult result = new AttemptGradingResult(
                    new BigDecimal("8.50"), GradeLevel.EXCELLENT, "Great work", ScoringMode.GEMINI_FLASH_LITE
            );
            when(attemptGradingGateway.gradeAttempt(any(), any())).thenReturn(result);
            when(examScoreRepository.save(any(ExamScore.class))).thenAnswer(inv -> inv.getArgument(0));

            ExamTurn turn = ExamTurn.createMainQuestion(attemptId, UUID.randomUUID(), UUID.randomUUID(), 1, "Q1", null);
            turn.submitAnswer("Student answer", null);

            ExamScore score = gradingService.gradeAndCompleteAttempt(attempt, List.of(turn));

            assertThat(score).isNotNull();
            assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
            assertThat(attempt.getTotalScore()).isEqualTo(new BigDecimal("8.50"));
            assertThat(attempt.getGradeLevel()).isEqualTo(GradeLevel.EXCELLENT);
            verify(examAttemptRepository).save(attempt);
        }

        @Test
        @DisplayName("gradeAndCompleteAttempt transitions to INTERRUPTED and returns null when unrecoverable error occurs")
        void gradingFailureTransitionsToInterrupted() {
            ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
            org.springframework.test.util.ReflectionTestUtils.setField(attempt, "id", attemptId);
            attempt.start(true);
            attempt.finish(); // FINALIZING

            when(attemptGradingGateway.gradeAttempt(any(), any())).thenThrow(new RuntimeException("Gateway offline"));
            when(ruleBasedGraderPort.grade(any(), any())).thenThrow(new RuntimeException("Fallback failed"));

            ExamTurn turn = ExamTurn.createMainQuestion(attemptId, UUID.randomUUID(), UUID.randomUUID(), 1, "Q1", null);

            ExamScore score = gradingService.gradeAndCompleteAttempt(attempt, List.of(turn));

            assertThat(score).isNull();
            assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
            verify(examAttemptRepository).save(attempt);
        }
    }
}
