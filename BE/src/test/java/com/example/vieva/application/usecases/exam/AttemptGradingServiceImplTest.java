package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.output.AttemptGradingGateway;
import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.application.ports.output.ExamAttemptQuestionSnapshotRepository;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.RuleBasedGraderPort;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamAttemptQuestionSnapshot;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.GradeLevel;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.ScoringMode;
import com.example.vieva.domain.entities.TurnStatus;
import com.example.vieva.domain.entities.TurnType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttemptGradingServiceImplTest {

    @Mock
    private AttemptGradingGateway attemptGradingGateway;

    @Mock
    private RuleBasedGraderPort ruleBasedGraderPort;

    @Mock
    private RubricRepository rubricRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamScoreRepository examScoreRepository;

    @Mock
    private ExamAttemptQuestionSnapshotRepository snapshotRepository;

    private TrackingTransactionRunner transactionRunner;
    private AttemptGradingServiceImpl gradingService;

    private UUID attemptId;
    private UUID studentId;
    private UUID subjectId;
    private ExamAttempt attempt;

    static class TrackingTransactionRunner implements TransactionRunnerPort {
        private final AtomicBoolean transactionActive = new AtomicBoolean(false);
        private final AtomicInteger transactionCount = new AtomicInteger(0);

        public boolean isTransactionActive() {
            return transactionActive.get();
        }

        public int getTransactionCount() {
            return transactionCount.get();
        }

        @Override
        public <T> T inNewTransaction(Supplier<T> work) {
            transactionActive.set(true);
            transactionCount.incrementAndGet();
            try {
                return work.get();
            } finally {
                transactionActive.set(false);
            }
        }
    }

    @BeforeEach
    void setUp() {
        attemptId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        subjectId = UUID.randomUUID();

        attempt = ExamAttempt.builder()
                .id(attemptId)
                .studentId(studentId)
                .subjectId(subjectId)
                .status(AttemptStatus.FINALIZING)
                .currentQuestionIndex(0)
                .totalQuestions(1)
                .remainingSeconds(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        transactionRunner = new TrackingTransactionRunner();

        gradingService = new AttemptGradingServiceImpl(
                attemptGradingGateway,
                ruleBasedGraderPort,
                rubricRepository,
                examAttemptRepository,
                examScoreRepository,
                snapshotRepository,
                transactionRunner
        );
    }

    @Test
    @DisplayName("P1 Safety: Assert that no active database transaction or lock is held during slow Gateway call")
    void noActiveTransactionDuringGatewayCall() {
        UUID questionVersionId = UUID.randomUUID();
        ExamTurn turn = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(UUID.randomUUID())
                .questionVersionId(questionVersionId)
                .turnType(TurnType.MAIN_QUESTION)
                .turnOrder(1)
                .questionText("Explain polymorphism in OOP.")
                .transcriptText("Polymorphism allows objects to be treated as instances of their parent class.")
                .status(TurnStatus.EVALUATED)
                .score(new BigDecimal("9.00"))
                .build();
        List<ExamTurn> turns = List.of(turn);

        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(rubricRepository.findByQuestionVersionId(questionVersionId)).thenReturn(Optional.empty());
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of());

        AtomicBoolean gatewayExecutedOutsideTransaction = new AtomicBoolean(false);
        when(attemptGradingGateway.gradeAttempt(any(), any())).thenAnswer(invocation -> {
            boolean activeInsideGateway = transactionRunner.isTransactionActive();
            if (!activeInsideGateway) {
                gatewayExecutedOutsideTransaction.set(true);
            }
            return new AttemptGradingResult(
                    new BigDecimal("9.00"),
                    "EXCELLENT",
                    "Detailed feedback for student",
                    "GEMINI_FLASH_LITE"
            );
        });

        when(examScoreRepository.save(any(ExamScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamScore savedScore = gradingService.gradeAndCompleteAttempt(attempt, turns);

        assertThat(savedScore).isNotNull();
        assertThat(savedScore.getTotalScore()).isEqualByComparingTo("9.00");
        assertThat(savedScore.getGrade()).isEqualTo(GradeLevel.EXCELLENT);
        assertThat(savedScore.getScoringMode()).isEqualTo(ScoringMode.GEMINI_FLASH_LITE);

        // Core verification: Gateway MUST have executed outside any transaction
        assertThat(gatewayExecutedOutsideTransaction.get())
                .as("Gateway must be invoked outside any transaction context")
                .isTrue();

        // Exactly 2 transactions: Step 1 (read/prepare) and Step 3 (save/finalize)
        assertThat(transactionRunner.getTransactionCount()).isEqualTo(2);

        // Verify attempt transitioned to COMPLETED and recorded score
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(attempt.getTotalScore()).isEqualByComparingTo("9.00");
        assertThat(attempt.getGradeLevel()).isEqualTo(GradeLevel.EXCELLENT);
    }

    @Test
    @DisplayName("Idempotency: Returning existing score when already graded in Step 1, score saved exactly once")
    void concurrentFinalizationYieldsSingleSavedScore_whenScoreAlreadyExistsInStep1() {
        ExamScore existingScore = ExamScore.create(
                attemptId,
                new BigDecimal("8.50"),
                GradeLevel.EXCELLENT,
                "Pre-existing score",
                ScoringMode.GEMINI_FLASH_LITE
        );

        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.of(existingScore));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamScore result = gradingService.gradeAndCompleteAttempt(attempt, List.of());

        assertThat(result).isSameAs(existingScore);
        verify(attemptGradingGateway, never()).gradeAttempt(any(), any());
        verify(ruleBasedGraderPort, never()).grade(any(), any());
        verify(examScoreRepository, never()).save(any());
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(transactionRunner.getTransactionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Concurrency: Double-checked idempotency under lock in Step 3 prevents duplicate score saves")
    void concurrentFinalizationYieldsSingleSavedScore_whenConcurrentCompletionOccursInStep3() {
        ExamTurn turn = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .turnOrder(1)
                .questionText("Question 1")
                .transcriptText("Answer 1")
                .build();
        List<ExamTurn> turns = List.of(turn);

        ExamScore concurrentScore = ExamScore.create(
                attemptId,
                new BigDecimal("8.00"),
                GradeLevel.GOOD,
                "Concurrent thread saved score",
                ScoringMode.GEMINI_FLASH_LITE
        );

        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        // Step 1: No score yet
        // Step 3: Concurrent thread has saved score in the meantime
        when(examScoreRepository.findByAttemptId(attemptId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentScore));

        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of());
        when(attemptGradingGateway.gradeAttempt(any(), any())).thenReturn(
                new AttemptGradingResult(new BigDecimal("8.00"), "GOOD", "Feedback", "GEMINI_FLASH_LITE")
        );
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamScore result = gradingService.gradeAndCompleteAttempt(attempt, turns);

        assertThat(result).isSameAs(concurrentScore);
        verify(examScoreRepository, never()).save(any(ExamScore.class));
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(transactionRunner.getTransactionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Resilience: Gateway timeout/exception triggers fallback to RuleBasedGraderPort")
    void timeoutFallbackToRuleBased() {
        ExamTurn turn = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .turnOrder(1)
                .questionText("Question text")
                .transcriptText("Student answer")
                .score(new BigDecimal("7.00"))
                .status(TurnStatus.EVALUATED)
                .build();
        List<ExamTurn> turns = List.of(turn);

        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of());

        // Gateway times out or throws RuntimeException
        when(attemptGradingGateway.gradeAttempt(any(), any()))
                .thenThrow(new RuntimeException("Gateway call timed out after 30000ms"));

        when(ruleBasedGraderPort.grade(any(), any())).thenReturn(
                new AttemptGradingResult(
                        new BigDecimal("7.00"),
                        "GOOD",
                        "Fallback evaluation applied due to gateway timeout",
                        "RULE_BASED_FALLBACK"
                )
        );

        when(examScoreRepository.save(any(ExamScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamScore savedScore = gradingService.gradeAndCompleteAttempt(attempt, turns);

        assertThat(savedScore).isNotNull();
        assertThat(savedScore.getTotalScore()).isEqualByComparingTo("7.00");
        assertThat(savedScore.getGrade()).isEqualTo(GradeLevel.GOOD);
        assertThat(savedScore.getScoringMode()).isEqualTo(ScoringMode.RULE_BASED_FALLBACK);

        verify(attemptGradingGateway).gradeAttempt(any(), any());
        verify(ruleBasedGraderPort).grade(any(), any());
        verify(examScoreRepository).save(any(ExamScore.class));
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
    }

    @Test
    @DisplayName("Resilience: When gateway fails and ruleBasedGraderPort is null, fallbackRuleBasedGrading succeeds")
    void fallbackToInternalRuleBasedWhenGatewayFailsAndRulePortIsNull() {
        AttemptGradingServiceImpl serviceWithoutRulePort = new AttemptGradingServiceImpl(
                attemptGradingGateway,
                null, // No rule-based grader port
                rubricRepository,
                examAttemptRepository,
                examScoreRepository,
                snapshotRepository,
                transactionRunner
        );

        ExamTurn turn = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .turnOrder(1)
                .score(new BigDecimal("6.00"))
                .status(TurnStatus.EVALUATED)
                .build();
        List<ExamTurn> turns = List.of(turn);

        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of());

        when(attemptGradingGateway.gradeAttempt(any(), any()))
                .thenThrow(new RuntimeException("Gateway unreachable"));

        when(examScoreRepository.save(any(ExamScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamScore savedScore = serviceWithoutRulePort.gradeAndCompleteAttempt(attempt, turns);

        assertThat(savedScore).isNotNull();
        assertThat(savedScore.getTotalScore()).isEqualByComparingTo("6.00");
        assertThat(savedScore.getGrade()).isEqualTo(GradeLevel.AVERAGE);
        assertThat(savedScore.getScoringMode()).isEqualTo(ScoringMode.RULE_BASED_FALLBACK);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
    }

    @Test
    @DisplayName("Contract: If configured ruleBasedGraderPort throws unrecoverable exception, transitions to INTERRUPTED")
    void ruleBasedGraderFailureTransitionsToInterrupted() {
        ExamTurn turn = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .turnOrder(1)
                .score(new BigDecimal("6.00"))
                .status(TurnStatus.EVALUATED)
                .build();
        List<ExamTurn> turns = List.of(turn);

        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of());

        when(attemptGradingGateway.gradeAttempt(any(), any()))
                .thenThrow(new RuntimeException("Gateway unreachable"));
        when(ruleBasedGraderPort.grade(any(), any()))
                .thenThrow(new RuntimeException("Rule grader fatal calculation exception"));

        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamScore result = gradingService.gradeAndCompleteAttempt(attempt, turns);

        assertThat(result).isNull();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
        assertThat(attempt.getCompletedAt()).isNotNull();

        verify(examScoreRepository, never()).save(any(ExamScore.class));
        ArgumentCaptor<ExamAttempt> captor = ArgumentCaptor.forClass(ExamAttempt.class);
        verify(examAttemptRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
    }

    @Test
    @DisplayName("Error Handling: Unrecoverable error transitions attempt from FINALIZING to INTERRUPTED via syncError()")
    void failureStateTransitionsToInterrupted() {
        when(examAttemptRepository.findByIdForUpdate(attemptId))
                .thenThrow(new RuntimeException("Fatal database connection exception during Step 1"));

        // Error recovery re-fetches attempt via findById
        when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamScore result = gradingService.gradeAndCompleteAttempt(attempt, List.of());

        assertThat(result).isNull();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
        assertThat(attempt.getCompletedAt()).isNotNull();

        ArgumentCaptor<ExamAttempt> captor = ArgumentCaptor.forClass(ExamAttempt.class);
        verify(examAttemptRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
    }

    @Test
    @DisplayName("Score Aggregation: Snapshots calculate deterministic totalScore and gradeLevel across turns")
    void deterministicSnapshotAggregation() {
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();

        ExamAttemptQuestionSnapshot snap1 = ExamAttemptQuestionSnapshot.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(q1Id)
                .orderIndex(0)
                .questionText("Question 1")
                .build();

        ExamAttemptQuestionSnapshot snap2 = ExamAttemptQuestionSnapshot.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(q2Id)
                .orderIndex(1)
                .questionText("Question 2")
                .build();

        // Q1: Turn 1 (score 8.0), Turn 2 follow-up (score 9.0) -> last evaluated is 9.0
        ExamTurn turn1 = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(q1Id)
                .turnOrder(1)
                .status(TurnStatus.EVALUATED)
                .score(new BigDecimal("8.00"))
                .build();

        ExamTurn turn2 = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(q1Id)
                .turnOrder(2)
                .turnType(TurnType.FOLLOW_UP)
                .status(TurnStatus.EVALUATED)
                .score(new BigDecimal("9.00"))
                .build();

        // Q2: Turn 3 (score 5.0, teacherScore 7.0) -> teacherScore takes precedence
        ExamTurn turn3 = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(q2Id)
                .turnOrder(3)
                .status(TurnStatus.EVALUATED)
                .score(new BigDecimal("5.00"))
                .teacherScore(new BigDecimal("7.00"))
                .build();

        List<ExamTurn> turns = List.of(turn1, turn2, turn3);

        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap1, snap2));

        // Gateway returns 10.00, but snapshot aggregation must override with deterministic average:
        // Q1 = 9.00, Q2 = 7.00 -> Average = 8.00 -> GradeLevel.GOOD (>= 7.0 and < 8.5)
        when(attemptGradingGateway.gradeAttempt(any(), any())).thenReturn(
                new AttemptGradingResult(new BigDecimal("10.00"), "EXCELLENT", "Great", "GEMINI_FLASH_LITE")
        );

        when(examScoreRepository.save(any(ExamScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamScore savedScore = gradingService.gradeAndCompleteAttempt(attempt, turns);

        assertThat(savedScore).isNotNull();
        assertThat(savedScore.getTotalScore()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(savedScore.getGrade()).isEqualTo(GradeLevel.GOOD);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
        assertThat(attempt.getTotalScore()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(attempt.getGradeLevel()).isEqualTo(GradeLevel.GOOD);
    }

    @Test
    @DisplayName("Validation: Returns null when attempt is null")
    void returnsNullWhenAttemptIsNull() {
        ExamScore result = gradingService.gradeAndCompleteAttempt(null, List.of());
        assertThat(result).isNull();
        verifyNoInteractions(examAttemptRepository, examScoreRepository, attemptGradingGateway);
    }

    @Test
    @DisplayName("P2 Safety: AttemptGradingServiceImpl fails fast when TransactionRunnerPort is null")
    void constructorFailsFastWhenTransactionRunnerIsNull() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new AttemptGradingServiceImpl(
                attemptGradingGateway,
                ruleBasedGraderPort,
                rubricRepository,
                examAttemptRepository,
                examScoreRepository,
                snapshotRepository,
                null
        )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("transactionRunner must not be null");
    }

    @Test
    @DisplayName("P1 Regression: Gateway blocked while another transaction changes status to INTERRUPTED -> assert no score or state overwrite")
    void gatewayBlockedWhileAnotherTxChangesStatusToInterrupted_assertNoScoreOrStateOverwrite() {
        UUID questionVersionId = UUID.randomUUID();
        ExamTurn turn = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(UUID.randomUUID())
                .questionVersionId(questionVersionId)
                .turnOrder(1)
                .status(TurnStatus.EVALUATED)
                .turnType(TurnType.MAIN_QUESTION)
                .score(new BigDecimal("7.50"))
                .transcriptText("Student response")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ExamAttempt interruptedAttempt = attempt.toBuilder()
                .status(AttemptStatus.INTERRUPTED)
                .completedAt(Instant.now())
                .build();

        when(examAttemptRepository.findByIdForUpdate(attemptId))
                .thenReturn(Optional.of(attempt))
                .thenReturn(Optional.of(interruptedAttempt));

        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of());

        when(attemptGradingGateway.gradeAttempt(any(), any())).thenAnswer(inv -> {
            attempt.cancel();
            return new AttemptGradingResult(new BigDecimal("8.50"), "EXCELLENT", "Great job", "GEMINI_FLASH_LITE");
        });

        ExamScore returnedScore = gradingService.gradeAndCompleteAttempt(attempt, List.of(turn));

        assertThat(returnedScore).isNull();
        verify(examScoreRepository, never()).save(any(ExamScore.class));
        verify(examAttemptRepository, never()).save(argThat(a -> a.getStatus() == AttemptStatus.COMPLETED));
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.INTERRUPTED);
        assertThat(attempt.getTotalScore()).isNull();
    }

    @Test
    @DisplayName("P1 Regression: Returns null and does not resurrect attempt when row does not exist in repository")
    void missingAttemptInRepositoryReturnsNullWithoutResurrection() {
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.empty());

        ExamScore returnedScore = gradingService.gradeAndCompleteAttempt(attempt, List.of());

        assertThat(returnedScore).isNull();
        verifyNoInteractions(attemptGradingGateway);
        verify(examScoreRepository, never()).save(any());
        verify(examAttemptRepository, never()).save(any());
    }
}
