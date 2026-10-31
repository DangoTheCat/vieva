package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.common.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.input.exam.TurnEvaluationView;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.domain.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitTurnAnswerUseCaseTest {

    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private ExamTurnRepository examTurnRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionVersionRepository questionVersionRepository;
    @Mock
    private RubricRepository rubricRepository;
    @Mock
    private FileStoragePort fileStoragePort;
    @Mock
    private VivaEvaluationGateway vivaEvaluationGateway;
    @Mock
    private AttemptGradingService attemptGradingService;
    @Mock
    private ExamAttemptQuestionSnapshotRepository snapshotRepository;
    @Mock
    private ExamFinalizationPort examFinalizationPort;
    @Mock
    private CourseDocumentRepository courseDocumentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private EmbeddingModelPort embeddingModel;

    private SubmitTurnAnswerUseCaseImpl useCase;

    private final UUID studentId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID turnId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID questionId2 = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID versionId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new SubmitTurnAnswerUseCaseImpl(
                examAttemptRepository,
                examTurnRepository,
                questionRepository,
                questionVersionRepository,
                rubricRepository,
                fileStoragePort,
                vivaEvaluationGateway,
                attemptGradingService,
                snapshotRepository,
                examFinalizationPort,
                courseDocumentRepository,
                documentChunkRepository,
                embeddingModel,
                new TransactionRunnerPort() {
                    @Override
                    public <T> T inNewTransaction(java.util.function.Supplier<T> work) {
                        return work.get();
                    }
                }
        );
    }

    @Test
    @DisplayName("submitAnswer rejects submission if attempt is not IN_PROGRESS")
    void submitAnswerRejectsWhenNotInProgress() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 2);
        attempt.setId(attemptId); // Status is READY
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Question", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));

        assertThatThrownBy(() -> useCase.submitAnswer(attemptId, turnId, studentId, "Answer", null, null))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessageContaining("Exam attempt is not IN_PROGRESS");
    }

    @Test
    @DisplayName("submitAnswer rejects empty transcript and audio")
    void submitAnswerRejectsEmptyTranscriptAndAudio() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 2);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Question", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));

        assertThatThrownBy(() -> useCase.submitAnswer(attemptId, turnId, studentId, "   ", null, ""))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessageContaining("Either transcript text or student audio must be provided");
    }

    @Test
    @DisplayName("submitAnswer returns idempotent view when turn is already EVALUATED")
    void submitAnswerReturnsIdempotentViewWhenTurnAlreadyEvaluated() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 2);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "What is Spring?", null);
        turn.setId(turnId);
        turn.submitAnswer("Prior answer", null);
        turn.evaluate(new BigDecimal("8.50"), "Great answer", "{\"score\": 8.5}");
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));

        TurnEvaluationView view = useCase.submitAnswer(attemptId, turnId, studentId, "Repeat answer", null, null);

        assertThat(view).isNotNull();
        assertThat(view.turnId()).isEqualTo(turnId);
        assertThat(view.score()).isEqualByComparingTo(new BigDecimal("8.50"));
        assertThat(view.feedback()).isEqualTo("Great answer");

        // Viva gateway must not be called again
        verifyNoInteractions(vivaEvaluationGateway);
    }

    @Test
    @DisplayName("submitAnswer with snapshots advances to next snapshot question and never searches bank")
    void submitAnswerWithSnapshotsAdvancesToNextSnapshotQuestion() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 2);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn1 = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "What is Spring?", null);
        turn1.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn1));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptQuestionSnapshot snap1 = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId, versionId, 0, "What is Spring?", "IoC container", "{\"criteria\":[]}"
        );
        ExamAttemptQuestionSnapshot snap2 = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId2, versionId2, 1, "What is Hibernate?", "ORM tool", "{\"criteria\":[]}"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap1, snap2));

        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                false, false, null, new BigDecimal("9.00"), "Clear answer", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(evalResult);

        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn1));

        TurnEvaluationView view = useCase.submitAnswer(attemptId, turnId, studentId, "Spring is an IoC container", null, null);

        assertThat(view).isNotNull();
        assertThat(view.isAttemptFinished()).isFalse();
        assertThat(view.nextQuestionText()).isEqualTo("What is Hibernate?");
        assertThat(view.nextTurnId()).isNotNull();

        // Verify that questionRepository.searchBank was NEVER called
        verify(questionRepository, never()).searchBank(any());
        // Verify next turn was created
        verify(examTurnRepository, atLeast(2)).save(any(ExamTurn.class));
    }

    @Test
    @DisplayName("submitAnswer passes frozen rubric and expected answer in VivaEvaluationContext")
    void submitAnswerPassesFrozenRubricAndExpectedAnswerInVivaEvaluationContext() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Explain AOP", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        String rubricJson = "{\"criteria\":[{\"name\":\"clarity\",\"weight\":1.0}]}";
        ExamAttemptQuestionSnapshot snap = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId, versionId, 0, "Explain AOP", "Aspect Oriented Programming", rubricJson
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap));

        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                true, false, null, new BigDecimal("8.00"), "Well explained", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(evalResult);

        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

        useCase.submitAnswer(attemptId, turnId, studentId, "AOP modularizes cross-cutting concerns", null, null);

        ArgumentCaptor<VivaEvaluationContext> contextCaptor = ArgumentCaptor.forClass(VivaEvaluationContext.class);
        verify(vivaEvaluationGateway).evaluateTurn(any(), any(), contextCaptor.capture());

        VivaEvaluationContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext).isNotNull();
        assertThat(capturedContext.expectedAnswer()).isEqualTo("Aspect Oriented Programming");
        assertThat(capturedContext.rubricJson()).isEqualTo(rubricJson);
    }

    @Test
    @DisplayName("submitAnswer with snapshots finalizes and triggers post-commit finalization worker when done")
    void submitAnswerWithSnapshotsFinalizesWhenAllSnapshotQuestionsDone() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "What is Spring?", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptQuestionSnapshot snap = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId, versionId, 0, "What is Spring?", "IoC framework", "{}"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap));

        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                true, false, null, new BigDecimal("8.00"), "Good explanation", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(evalResult);

        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

        TurnEvaluationView view = useCase.submitAnswer(attemptId, turnId, studentId, "Spring is an IoC framework", null, null);

        assertThat(view).isNotNull();
        assertThat(view.isAttemptFinished()).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FINALIZING);

        // Verify attempt saved with FINALIZING status
        verify(examAttemptRepository).save(argThat(a -> a.getStatus() == AttemptStatus.FINALIZING));
        // Verify post-commit finalization port triggered
        verify(examFinalizationPort).dispatchFinalization(attemptId);
    }

    @Test
    @DisplayName("submitAnswer caps follow-up turns at maximum of two")
    void submitAnswerCapsFollowupsAtMaxTwo() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        // Create main turn + 2 existing follow-ups
        ExamTurn mainTurn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Main Q", null);
        mainTurn.setId(UUID.randomUUID());

        ExamTurn f1 = ExamTurn.createFollowUp(attemptId, questionId, versionId, mainTurn.getId(), 2, "Follow 1", null);
        f1.setId(UUID.randomUUID());

        ExamTurn f2 = ExamTurn.createFollowUp(attemptId, questionId, versionId, mainTurn.getId(), 3, "Follow 2", null);
        f2.setId(turnId); // This is the turn being answered now
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(f2));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptQuestionSnapshot snap = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId, versionId, 0, "Main Q", "Expected", "{}"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap));

        // AI wants a third follow up
        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                false, true, "Follow 3 text", new BigDecimal("7.00"), "Need more detail", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(evalResult);

        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(mainTurn, f1, f2));

        TurnEvaluationView view = useCase.submitAnswer(attemptId, turnId, studentId, "Followup answer 2", null, null);

        // Max 2 follow-ups reached -> should NOT create a 3rd follow-up, should finalize since snap is the only main question
        assertThat(view).isNotNull();
        assertThat(view.nextTurnType()).isNull();
        assertThat(view.isAttemptFinished()).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FINALIZING);
    }

    @Test
    @DisplayName("submitAnswer calls the LLM outside any transaction (the attempt row is not locked while waiting)")
    void submitAnswerEvaluatesOutsideTransaction() {
        boolean[] inTransaction = {false};
        int[] transactions = {0};
        SubmitTurnAnswerUseCaseImpl txAwareUseCase = new SubmitTurnAnswerUseCaseImpl(
                examAttemptRepository, examTurnRepository, questionRepository, questionVersionRepository,
                rubricRepository, fileStoragePort, vivaEvaluationGateway, attemptGradingService,
                snapshotRepository, examFinalizationPort, courseDocumentRepository, documentChunkRepository,
                embeddingModel,
                new TransactionRunnerPort() {
                    @Override
                    public <T> T inNewTransaction(java.util.function.Supplier<T> work) {
                        transactions[0]++;
                        inTransaction[0] = true;
                        try {
                            return work.get();
                        } finally {
                            inTransaction[0] = false;
                        }
                    }
                });

        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Q", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(
                ExamAttemptQuestionSnapshot.create(attemptId, questionId, versionId, 0, "Q", "Expected", "{}")));
        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenAnswer(inv -> {
            assertThat(inTransaction[0]).as("LLM must not run inside a transaction").isFalse();
            return new VivaEvaluationResult(true, false, null, new BigDecimal("9.00"), "Great", "{}");
        });

        TurnEvaluationView view = txAwareUseCase.submitAnswer(attemptId, turnId, studentId, "Answer", null, null);

        assertThat(view.status()).isEqualTo(TurnStatus.EVALUATED.name());
        assertThat(transactions[0]).isEqualTo(2);
    }

    @Test
    @DisplayName("submitAnswer closes the attempt without grading when the exam clock ran out beyond the grace window")
    void submitAnswerAfterTimeRanOutFinalizesWithoutGrading() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 2, 60);
        attempt.setId(attemptId);
        attempt.start(true);
        attempt.setResumedAt(java.time.Instant.now().minusSeconds(60 + ExamAttempt.SUBMIT_GRACE_SECONDS + 30));
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Q", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));

        TurnEvaluationView view = useCase.submitAnswer(attemptId, turnId, studentId, "Late answer", null, null);

        assertThat(view.isAttemptFinished()).isTrue();
        assertThat(view.remainingSeconds()).isZero();
        assertThat(turn.getStatus()).isEqualTo(TurnStatus.PENDING);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FINALIZING);
        verify(vivaEvaluationGateway, never()).evaluateTurn(any(), any(), any());
        verify(examFinalizationPort).dispatchFinalization(attemptId);
    }

    @Test
    @DisplayName("submitAnswer respects the follow-up limit frozen from the exam (1 instead of the default 2)")
    void submitAnswerUsesConfiguredFollowUpLimit() {
        ExamAttempt attempt = ExamAttempt.initForExam(UUID.randomUUID(), studentId, subjectId, 1, 900, 120, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn mainTurn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Main Q", null);
        mainTurn.setId(UUID.randomUUID());
        ExamTurn f1 = ExamTurn.createFollowUp(attemptId, questionId, versionId, mainTurn.getId(), 2, "Follow 1", null);
        f1.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(f1));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(
                ExamAttemptQuestionSnapshot.create(attemptId, questionId, versionId, 0, "Main Q", "Expected", "{}")));
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(new VivaEvaluationResult(
                false, true, "Follow 2 text", new BigDecimal("6.00"), "Unclear", "{}"));
        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(mainTurn, f1));

        TurnEvaluationView view = useCase.submitAnswer(attemptId, turnId, studentId, "Answer", null, null);

        assertThat(view.nextTurnType()).isNull();
        assertThat(view.isAttemptFinished()).isTrue();
    }

    @Test
    @DisplayName("submitAnswer flags an answer that arrives after the per-answer deadline")
    void submitAnswerFlagsLateAnswer() {
        ExamAttempt attempt = ExamAttempt.initForExam(UUID.randomUUID(), studentId, subjectId, 1, 900, 60, 2);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Q", null);
        turn.setId(turnId);
        turn.setAskedAt(java.time.Instant.now().minusSeconds(60 + ExamAttempt.SUBMIT_GRACE_SECONDS + 20));
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(
                ExamAttemptQuestionSnapshot.create(attemptId, questionId, versionId, 0, "Q", "Expected", "{}")));
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(new VivaEvaluationResult(
                true, false, null, new BigDecimal("8.00"), "Good", "{}"));
        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

        TurnEvaluationView view = useCase.submitAnswer(attemptId, turnId, studentId, "Answer", null, null);

        assertThat(view.timedOut()).isTrue();
        assertThat(turn.isTimedOut()).isTrue();
    }

    @Test
    @DisplayName("submitAnswer builds non-empty RAG context when ready subject documents and similar chunks exist")
    void submitAnswerWithAvailableSubjectEvidenceBuildsNonEmptyRagContext() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "What is polymorphism?", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID docId = UUID.randomUUID();
        CourseDocument doc = CourseDocument.builder()
                .documentId(docId)
                .subjectId(subjectId)
                .fileName("OOP_Principles.pdf")
                .indexingStatus(DocumentStatus.READY)
                .build();
        when(courseDocumentRepository.findActiveBySubjectId(subjectId)).thenReturn(List.of(doc));

        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingModel.generateEmbedding("What is polymorphism? It is the ability of an object to take many forms"))
                .thenReturn(embedding);

        ChunkSearchResult chunk = ChunkSearchResult.of(
                UUID.randomUUID(), docId, "OOP_Principles.pdf", 0, 1,
                "Polymorphism enables one interface to be used for a general class of actions.", 0.1
        );
        when(documentChunkRepository.searchSimilarChunks(eq(subjectId), eq(List.of(docId)), eq(embedding), eq(0.2), eq(3)))
                .thenReturn(List.of(chunk));

        ExamAttemptQuestionSnapshot snap = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId, versionId, 0, "What is polymorphism?", "Many forms", "{}"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap));
        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                true, false, null, new BigDecimal("9.00"), "Excellent", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(evalResult);

        useCase.submitAnswer(attemptId, turnId, studentId, "It is the ability of an object to take many forms", null, null);

        ArgumentCaptor<VivaEvaluationContext> contextCaptor = ArgumentCaptor.forClass(VivaEvaluationContext.class);
        verify(vivaEvaluationGateway).evaluateTurn(any(), any(), contextCaptor.capture());

        VivaEvaluationContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext).isNotNull();
        assertThat(capturedContext.ragContext()).isNotNull();
        assertThat(capturedContext.ragContext())
                .contains("[Tài liệu: OOP_Principles.pdf]")
                .contains("Polymorphism enables one interface");
    }

    @Test
    @DisplayName("submitAnswer queries correct subjectId and ready document IDs only")
    void submitAnswerQueriesCorrectSubjectAndReadyDocumentIdsOnly() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Explain encapsulation", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID readyDocId1 = UUID.randomUUID();
        UUID readyDocId2 = UUID.randomUUID();
        UUID indexingDocId = UUID.randomUUID();
        UUID deletedDocId = UUID.randomUUID();

        CourseDocument ready1 = CourseDocument.builder()
                .documentId(readyDocId1)
                .subjectId(subjectId)
                .fileName("Chapter1.pdf")
                .indexingStatus(DocumentStatus.READY)
                .build();
        CourseDocument ready2 = CourseDocument.builder()
                .documentId(readyDocId2)
                .subjectId(subjectId)
                .fileName("Chapter2.pdf")
                .indexingStatus(DocumentStatus.READY)
                .build();
        CourseDocument indexing = CourseDocument.builder()
                .documentId(indexingDocId)
                .subjectId(subjectId)
                .fileName("Chapter3.pdf")
                .indexingStatus(DocumentStatus.INDEXING)
                .build();
        CourseDocument deleted = CourseDocument.builder()
                .documentId(deletedDocId)
                .subjectId(subjectId)
                .fileName("Chapter4.pdf")
                .indexingStatus(DocumentStatus.READY)
                .deletedAt(java.time.Instant.now())
                .build();

        when(courseDocumentRepository.findActiveBySubjectId(subjectId))
                .thenReturn(List.of(ready1, ready2, indexing, deleted));

        float[] embedding = new float[]{0.5f, 0.6f};
        when(embeddingModel.generateEmbedding("Explain encapsulation Encapsulation binds code and data"))
                .thenReturn(embedding);

        when(documentChunkRepository.searchSimilarChunks(eq(subjectId), any(), eq(embedding), eq(0.2), eq(3)))
                .thenReturn(List.of());

        ExamAttemptQuestionSnapshot snap = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId, versionId, 0, "Explain encapsulation", "Data hiding", "{}"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap));
        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                true, false, null, new BigDecimal("8.00"), "Good", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(evalResult);

        useCase.submitAnswer(attemptId, turnId, studentId, "Encapsulation binds code and data", null, null);

        // Verify that only the two ready document IDs were passed
        verify(documentChunkRepository).searchSimilarChunks(
                eq(subjectId),
                argThat(ids -> ids.size() == 2 && ids.contains(readyDocId1) && ids.contains(readyDocId2)),
                eq(embedding),
                eq(0.2),
                eq(3)
        );
    }

    @Test
    @DisplayName("submitAnswer falls back to null ragContext when no documents or chunks below similarity")
    void submitAnswerFallsBackToNullRagContextWhenNoDocumentsOrBelowSimilarity() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Explain recursion", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        // Case A: No ready docs in subject
        when(courseDocumentRepository.findActiveBySubjectId(subjectId)).thenReturn(List.of());

        ExamAttemptQuestionSnapshot snap = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId, versionId, 0, "Explain recursion", "Self calling", "{}"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap));
        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                true, false, null, new BigDecimal("8.00"), "Good", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(evalResult);

        useCase.submitAnswer(attemptId, turnId, studentId, "A function calling itself", null, null);

        ArgumentCaptor<VivaEvaluationContext> contextCaptor = ArgumentCaptor.forClass(VivaEvaluationContext.class);
        verify(vivaEvaluationGateway).evaluateTurn(any(), any(), contextCaptor.capture());

        assertThat(contextCaptor.getValue()).isNotNull();
        assertThat(contextCaptor.getValue().ragContext()).isNull();
        verifyNoInteractions(documentChunkRepository);
    }

    @Test
    @DisplayName("submitAnswer never reads question bank when snapshots exist")
    void submitAnswerNeverReadsQuestionBankWhenSnapshotsExist() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true);
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "Snapshotted Q", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptQuestionSnapshot snap = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId, versionId, 0, "Snapshotted Q", "Expected answer", "{\"criteria\":[]}"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap));
        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                true, false, null, new BigDecimal("8.00"), "Good", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any(), any())).thenReturn(evalResult);

        useCase.submitAnswer(attemptId, turnId, studentId, "Student answer", null, null);

        // Verify question bank was NEVER queried
        verify(questionRepository, never()).searchBank(any());
        verify(questionRepository, never()).findById(any());
        verify(questionVersionRepository, never()).findById(any());
    }
}
