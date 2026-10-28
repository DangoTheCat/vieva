package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.*;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamQuestionRepository examQuestionRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionVersionRepository questionVersionRepository;
    @Mock
    private SubjectAccessGuard accessGuard;
    @Mock
    private UserRepository userRepository;

    private ExamServiceImpl examService;

    private final UUID actorId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examService = new ExamServiceImpl(
                examRepository,
                examQuestionRepository,
                examAttemptRepository,
                questionRepository,
                questionVersionRepository,
                accessGuard,
                userRepository
        );
    }

    @Test
    @DisplayName("createExam: successfully creates exam in DRAFT state")
    void createExam_Success() {
        CreateExamCommand cmd = new CreateExamCommand(subjectId, "Midterm Exam", 1200);
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamView view = examService.createExam(cmd, actorId);

        assertThat(view).isNotNull();
        assertThat(view.title()).isEqualTo("Midterm Exam");
        assertThat(view.durationSeconds()).isEqualTo(1200);
        assertThat(view.status()).isEqualTo(ExamStatus.DRAFT);
        verify(accessGuard).requireManage(actorId, subjectId);
    }

    @Test
    @DisplayName("updateExamTitle: rejects edit on non-DRAFT exam")
    void updateExamTitle_NonDraft_ThrowsException() {
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(actorId)
                .title("Midterm")
                .status(ExamStatus.IN_PROGRESS)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(exam));

        UpdateExamCommand cmd = new UpdateExamCommand("New Title");
        assertThatThrownBy(() -> examService.updateExamTitle(examId, cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_NOT_DRAFT);
    }

    @Test
    @DisplayName("updateExamStatus: rejects transition to IN_PROGRESS when question count is 0")
    void updateExamStatus_PublishEmptyExam_ThrowsException() {
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(actorId)
                .title("Midterm")
                .status(ExamStatus.DRAFT)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId)).thenReturn(Collections.emptyList());

        UpdateExamStatusCommand cmd = new UpdateExamStatusCommand(ExamStatus.IN_PROGRESS);
        assertThatThrownBy(() -> examService.updateExamStatus(examId, cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessageContaining("Cannot publish exam with zero questions");
    }

    @Test
    @DisplayName("updateExamStatus: refuses to publish an exam that contains an archived question")
    void updateExamStatus_PublishWithArchivedQuestion_Throws() {
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(actorId)
                .title("Midterm")
                .status(ExamStatus.DRAFT)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(exam));
        UUID questionId = UUID.randomUUID();
        when(examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId))
                .thenReturn(List.of(ExamQuestion.create(examId, questionId, UUID.randomUUID(), 0)));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(Question.builder()
                .questionId(questionId).subjectId(subjectId).status(QuestionLifecycleStatus.ARCHIVED).build()));

        assertThatThrownBy(() -> examService.updateExamStatus(examId, new UpdateExamStatusCommand(ExamStatus.IN_PROGRESS), actorId))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("archived");
    }

    @Test
    @DisplayName("updateExamStatus: successfully publishes exam when questions are configured")
    void updateExamStatus_PublishWithQuestions_Success() {
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(actorId)
                .title("Midterm")
                .status(ExamStatus.DRAFT)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID questionId = UUID.randomUUID();
        ExamQuestion eq = ExamQuestion.create(examId, questionId, UUID.randomUUID(), 0);
        when(examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId)).thenReturn(List.of(eq));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(Question.builder()
                .questionId(questionId).subjectId(subjectId).status(QuestionLifecycleStatus.ACTIVE).build()));

        UpdateExamStatusCommand cmd = new UpdateExamStatusCommand(ExamStatus.IN_PROGRESS);
        ExamView view = examService.updateExamStatus(examId, cmd, actorId);

        assertThat(view).isNotNull();
        assertThat(view.status()).isEqualTo(ExamStatus.IN_PROGRESS);
        assertThat(view.questionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteExam: cannot delete exam if attempts already exist")
    void deleteExam_WithAttempts_ThrowsException() {
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(actorId)
                .status(ExamStatus.DRAFT)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.countByExamId(examId)).thenReturn(3L);

        assertThatThrownBy(() -> examService.deleteExam(examId, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_NOT_DRAFT);
    }

    @Test
    @DisplayName("setExamQuestions: rejects question from different subject")
    void setExamQuestions_SubjectMismatch_ThrowsException() {
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(actorId)
                .status(ExamStatus.DRAFT)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.countByExamId(examId)).thenReturn(0L);

        UUID qId = UUID.randomUUID();
        UUID vId = UUID.randomUUID();
        UUID otherSubjectId = UUID.randomUUID();

        Question diffSubjectQ = Question.builder()
                .questionId(qId)
                .subjectId(otherSubjectId)
                .build();
        when(questionRepository.findById(qId)).thenReturn(Optional.of(diffSubjectQ));

        SetExamQuestionsCommand cmd = new SetExamQuestionsCommand(List.of(
                new SetExamQuestionsCommand.ExamQuestionItem(qId, vId, 0)
        ));

        assertThatThrownBy(() -> examService.setExamQuestions(examId, cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_QUESTION_MISMATCH);
    }

    @Test
    @DisplayName("checkExamAccess: rejects user who is neither exam creator nor admin")
    void checkExamAccess_NonCreator_ThrowsUnauthorized() {
        UUID otherCreatorId = UUID.randomUUID();
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(otherCreatorId)
                .title("Private Exam")
                .status(ExamStatus.DRAFT)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(exam));

        UpdateExamCommand cmd = new UpdateExamCommand("Attempted Rename");
        assertThatThrownBy(() -> examService.updateExamTitle(examId, cmd, actorId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("checkExamAccess: admin override allows managing exam created by someone else")
    void checkExamAccess_AdminOverride_Success() {
        UUID otherCreatorId = UUID.randomUUID();
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(otherCreatorId)
                .title("Original Title")
                .status(ExamStatus.DRAFT)
                .build();
        when(examRepository.findByIdForUpdate(examId)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        User adminUser = User.builder()
                .userId(adminId)
                .status(UserStatus.ACTIVE)
                .build();
        adminUser.assignRole(Role.builder().roleId(1).roleCode("ROLE_ADMIN").roleName("Admin").build(), null);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        UpdateExamCommand cmd = new UpdateExamCommand("Admin Updated Title");
        ExamView view = examService.updateExamTitle(examId, cmd, adminId);

        assertThat(view).isNotNull();
        assertThat(view.title()).isEqualTo("Admin Updated Title");
    }
}
