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

    private SubmitTurnAnswerUseCaseImpl useCase;

    private final UUID studentId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID turnId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();

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
                attemptGradingService
        );
    }

    @Test
    @DisplayName("submitAnswer rejects submission if attempt is not IN_PROGRESS")
    void submitAnswerRejectsWhenNotInProgress() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 2);
        attempt.setId(attemptId); // Status is READY
        when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> useCase.submitAnswer(attemptId, turnId, studentId, "Answer", null, null))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessageContaining("Exam attempt is not IN_PROGRESS");
    }

    @Test
    @DisplayName("submitAnswer transitions attempt to FINALIZING and invokes grading when all questions are done")
    void submitAnswerFinalizesWhenAllQuestionsDone() {
        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, 1);
        attempt.setId(attemptId);
        attempt.start(true); // Status is IN_PROGRESS
        when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        ExamTurn turn = ExamTurn.createMainQuestion(attemptId, questionId, versionId, 1, "What is Spring?", null);
        turn.setId(turnId);
        when(examTurnRepository.findById(turnId)).thenReturn(Optional.of(turn));
        when(examTurnRepository.save(any(ExamTurn.class))).thenAnswer(inv -> inv.getArgument(0));

        VivaEvaluationResult evalResult = new VivaEvaluationResult(
                true, false, null, new BigDecimal("8.00"), "Good explanation", "{}"
        );
        when(vivaEvaluationGateway.evaluateTurn(any(), any())).thenReturn(evalResult);

        // Only 1 question in bank, currentQuestionIndex is 0 -> index + 1 == 1 == size, so no more questions
        Question q = Question.builder().questionId(questionId).build();
        when(questionRepository.searchBank(any(QuestionBankSearchCriteria.class)))
                .thenReturn(PageResult.<Question>builder().content(List.of(q)).totalElements(1L).build());

        when(examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId)).thenReturn(List.of(turn));

        TurnEvaluationView view = useCase.submitAnswer(attemptId, turnId, studentId, "Spring is an IoC framework", null, null);

        assertThat(view).isNotNull();
        assertThat(view.isAttemptFinished()).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FINALIZING);

        // Verify attempt was transitioned to FINALIZING and saved
        verify(examAttemptRepository).save(argThat(a -> a.getStatus() == AttemptStatus.FINALIZING));
        // Verify grading service was called
        verify(attemptGradingService).gradeAndCompleteAttempt(eq(attempt), anyList());
    }
}
