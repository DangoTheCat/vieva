package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.*;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.domain.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamQuestionRepository examQuestionRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private ExamAttemptQuestionSnapshotRepository snapshotRepository;
    @Mock
    private ExamTurnRepository examTurnRepository;
    @Mock
    private QuestionVersionRepository questionVersionRepository;
    @Mock
    private RubricRepository rubricRepository;
    @Mock
    private RubricCriterionRepository rubricCriterionRepository;
    @Mock
    private JsonSerializerPort jsonSerializerPort;
    @Mock
    private SubjectRepository subjectRepository;

    private StudentExamServiceImpl service;

    private final UUID studentId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StudentExamServiceImpl(
                examRepository,
                examQuestionRepository,
                examAttemptRepository,
                snapshotRepository,
                examTurnRepository,
                questionVersionRepository,
                rubricRepository,
                rubricCriterionRepository,
                jsonSerializerPort,
                subjectRepository
        );
    }

    @Test
    @DisplayName("enterExam: idempotent, returns existing attempt if already entered under lock")
    void enterExam_ExistingAttempt_ReturnsExisting() {
        Exam activeExam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .status(ExamStatus.IN_PROGRESS)
                .durationSeconds(900)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(activeExam));

        ExamAttempt existing = ExamAttempt.builder()
                .id(UUID.randomUUID())
                .examId(examId)
                .studentId(studentId)
                .subjectId(subjectId)
                .status(AttemptStatus.READY)
                .currentQuestionIndex(0)
                .totalQuestions(3)
                .build();

        when(examAttemptRepository.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.of(existing));

        ExamAttemptView view = service.enterExam(examId, studentId);

        assertThat(view).isNotNull();
        assertThat(view.attemptId()).isEqualTo(existing.getId());
        verify(examRepository).findByIdForUpdate(examId);
    }

    @Test
    @DisplayName("enterExam: closed exam rejects new entrants with EXAM_CLOSED")
    void enterExam_ClosedExam_ThrowsExamClosed() {
        Exam closedExam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .status(ExamStatus.COMPLETED)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(closedExam));

        assertThatThrownBy(() -> service.enterExam(examId, studentId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_CLOSED);
    }

    @Test
    @DisplayName("enterExam: in-progress exam locks target, freezes snapshots and returns READY attempt")
    void enterExam_Success_FreezesQuestions() {
        Exam activeExam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .status(ExamStatus.IN_PROGRESS)
                .durationSeconds(900)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(activeExam));

        when(examAttemptRepository.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.empty());

        UUID qId = UUID.randomUUID();
        UUID vId = UUID.randomUUID();
        ExamQuestion eq = ExamQuestion.create(examId, qId, vId, 0);
        when(examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId)).thenReturn(List.of(eq));

        QuestionVersion ver = QuestionVersion.builder()
                .questionVersionId(vId)
                .questionContent("Sample question text")
                .referenceAnswer("Sample answer")
                .build();
        when(questionVersionRepository.findById(vId)).thenReturn(Optional.of(ver));
        when(rubricRepository.findByQuestionVersionId(vId)).thenReturn(Optional.empty());

        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptView view = service.enterExam(examId, studentId);

        assertThat(view).isNotNull();
        assertThat(view.status()).isEqualTo(AttemptStatus.READY.name());
        verify(examRepository).findByIdForUpdate(examId);
        verify(snapshotRepository).saveAll(argThat(snapshots -> snapshots.size() == 1));
        verify(examTurnRepository).save(any(ExamTurn.class));
    }

    @Test
    @DisplayName("getAttemptQuestions: returns frozen questions without leaking expected answer or rubric")
    void getAttemptQuestions_HidesAnswers() {
        UUID attemptId = UUID.randomUUID();
        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .studentId(studentId)
                .build();
        when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        ExamAttemptQuestionSnapshot snap = ExamAttemptQuestionSnapshot.create(
                attemptId, UUID.randomUUID(), UUID.randomUUID(), 0,
                "What is dependency inversion?", "Secret answer", "[{\"criteria\":\"strict\"}]"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap));

        List<StudentFrozenQuestionView> result = service.getAttemptQuestions(attemptId, studentId);

        assertThat(result).hasSize(1);
        StudentFrozenQuestionView view = result.get(0);
        assertThat(view.questionText()).isEqualTo("What is dependency inversion?");
    }
}
