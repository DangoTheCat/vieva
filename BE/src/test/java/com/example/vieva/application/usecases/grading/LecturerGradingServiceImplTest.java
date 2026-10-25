package com.example.vieva.application.usecases.grading;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.grading.*;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LecturerGradingServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private LecturerSubjectRepository lecturerSubjectRepository;
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
    private ExamScoreRepository examScoreRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubjectAccessGuard accessGuard;
    @Mock
    private QuestionBankAuditor auditor;

    private LecturerGradingServiceImpl service;

    private final UUID lecturerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID questionId1 = UUID.randomUUID();
    private final UUID questionId2 = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LecturerGradingServiceImpl(
                subjectRepository,
                lecturerSubjectRepository,
                examRepository,
                examQuestionRepository,
                examAttemptRepository,
                snapshotRepository,
                examTurnRepository,
                examScoreRepository,
                userRepository,
                accessGuard,
                auditor
        );
    }

    @Test
    @DisplayName("gradeQuestion: sets teacher score on turn, recalculates attempt average across main questions, and clears override")
    void gradeQuestion_Success() {
        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .examId(examId)
                .subjectId(subjectId)
                .studentId(studentId)
                .status(AttemptStatus.COMPLETED)
                .totalQuestions(2)
                .build();
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(lecturerId)
                .title("Midterm Exam")
                .build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        ExamTurn turn1 = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId1)
                .turnType(TurnType.MAIN_QUESTION)
                .turnOrder(1)
                .status(TurnStatus.ANSWERED)
                .build();
        when(examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, questionId1))
                .thenReturn(List.of(turn1));

        ExamTurn turn2 = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId2)
                .turnType(TurnType.MAIN_QUESTION)
                .turnOrder(2)
                .status(TurnStatus.EVALUATED)
                .score(new BigDecimal("6.00"))
                .build();
        when(examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, questionId2))
                .thenReturn(List.of(turn2));

        ExamAttemptQuestionSnapshot snap1 = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId1, UUID.randomUUID(), 0, "Q1", "A1", "[]"
        );
        ExamAttemptQuestionSnapshot snap2 = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId2, UUID.randomUUID(), 1, "Q2", "A2", "[]"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap1, snap2));

        ExamScore score = ExamScore.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .totalScore(new BigDecimal("7.00"))
                .overrideTotalScore(new BigDecimal("9.00"))
                .overrideReason("Old override")
                .build();
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.of(score));

        User student = User.builder()
                .userId(studentId)
                .fullName("John Doe")
                .email("john@example.com")
                .build();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        GradeQuestionCommand cmd = new GradeQuestionCommand(new BigDecimal("8.00"), "Well reasoned");
        AttemptGradingDetailView result = service.gradeQuestion(attemptId, questionId1, cmd, lecturerId);

        assertThat(result).isNotNull();
        // Turn 1 given 8.00 by teacher, Turn 2 has 6.00 AI score -> avg = (8.00 + 6.00) / 2 = 7.00 (GOOD)
        verify(examTurnRepository).save(argThat(t -> new BigDecimal("8.00").compareTo(t.getTeacherScore()) == 0));
        verify(examAttemptRepository).save(argThat(a -> new BigDecimal("7.00").compareTo(a.getTotalScore()) == 0
                && a.getGradeLevel() == GradeLevel.GOOD));
        // Manual override cleared
        verify(examScoreRepository).save(argThat(s -> s.getOverrideTotalScore() == null && s.getOverrideReason() == null));
    }

    @Test
    @DisplayName("gradeQuestion: unanswered question counts as zero in aggregate score calculation")
    void gradeQuestion_UnansweredQuestionCountsAsZero() {
        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .examId(examId)
                .subjectId(subjectId)
                .studentId(studentId)
                .status(AttemptStatus.COMPLETED)
                .totalQuestions(2)
                .build();
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(lecturerId)
                .title("Midterm Exam")
                .build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        ExamTurn turn1 = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId1)
                .turnType(TurnType.MAIN_QUESTION)
                .turnOrder(1)
                .status(TurnStatus.ANSWERED)
                .build();
        when(examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, questionId1))
                .thenReturn(List.of(turn1));
        // Question 2 has no turns (unanswered)
        when(examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, questionId2))
                .thenReturn(Collections.emptyList());

        ExamAttemptQuestionSnapshot snap1 = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId1, UUID.randomUUID(), 0, "Q1", "A1", "[]"
        );
        ExamAttemptQuestionSnapshot snap2 = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId2, UUID.randomUUID(), 1, "Q2", "A2", "[]"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap1, snap2));

        ExamScore score = ExamScore.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .build();
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.of(score));

        User student = User.builder()
                .userId(studentId)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .build();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        GradeQuestionCommand cmd = new GradeQuestionCommand(new BigDecimal("10.00"), "Perfect answer");
        AttemptGradingDetailView result = service.gradeQuestion(attemptId, questionId1, cmd, lecturerId);

        assertThat(result).isNotNull();
        // Q1 = 10.00, Q2 = 0.00 -> avg = (10.00 + 0.00) / 2 = 5.00 (AVERAGE)
        verify(examAttemptRepository).save(argThat(a -> new BigDecimal("5.00").compareTo(a.getTotalScore()) == 0
                && a.getGradeLevel() == GradeLevel.AVERAGE));
    }

    @Test
    @DisplayName("gradeQuestion: rejects when attempt is not COMPLETED")
    void gradeQuestion_RejectsWhenAttemptNotCompleted() {
        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .examId(examId)
                .subjectId(subjectId)
                .studentId(studentId)
                .status(AttemptStatus.IN_PROGRESS)
                .totalQuestions(1)
                .build();
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(lecturerId)
                .build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        GradeQuestionCommand cmd = new GradeQuestionCommand(new BigDecimal("8.00"), "Good");

        assertThatThrownBy(() -> service.gradeQuestion(attemptId, questionId1, cmd, lecturerId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessageContaining("Grading is only permitted on completed attempts");
    }

    @Test
    @DisplayName("gradeQuestion: rejects score outside [0, 10]")
    void gradeQuestion_RejectsScoreOutsideRange() {
        GradeQuestionCommand cmdNegative = new GradeQuestionCommand(new BigDecimal("-1.00"), "Invalid");
        assertThatThrownBy(() -> service.gradeQuestion(attemptId, questionId1, cmdNegative, lecturerId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SCORE);

        GradeQuestionCommand cmdOverMax = new GradeQuestionCommand(new BigDecimal("10.50"), "Invalid");
        assertThatThrownBy(() -> service.gradeQuestion(attemptId, questionId1, cmdOverMax, lecturerId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SCORE);
    }

    @Test
    @DisplayName("gradeQuestion: rejects when lecturer is not exam creator and not admin")
    void gradeQuestion_RejectsWhenNotCreator() {
        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .examId(examId)
                .subjectId(subjectId)
                .studentId(studentId)
                .status(AttemptStatus.COMPLETED)
                .build();
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        UUID anotherLecturerId = UUID.randomUUID();
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(anotherLecturerId)
                .build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        GradeQuestionCommand cmd = new GradeQuestionCommand(new BigDecimal("8.00"), "Good");

        assertThatThrownBy(() -> service.gradeQuestion(attemptId, questionId1, cmd, lecturerId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GRADING_FORBIDDEN);
    }

    @Test
    @DisplayName("gradeQuestion: admin has global override and can grade even if not exam creator")
    void gradeQuestion_AdminGlobalOverride() {
        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .examId(examId)
                .subjectId(subjectId)
                .studentId(studentId)
                .status(AttemptStatus.COMPLETED)
                .totalQuestions(1)
                .build();
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(UUID.randomUUID()) // Created by someone else
                .title("Admin Exam")
                .build();

        // Admin user mock
        User adminUser = User.builder()
                .userId(adminId)
                .status(UserStatus.ACTIVE)
                .build();
        adminUser.assignRole(Role.builder().roleId(1).roleCode("ROLE_ADMIN").roleName("Admin").build(), null);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        ExamTurn turn1 = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId1)
                .turnType(TurnType.MAIN_QUESTION)
                .turnOrder(1)
                .status(TurnStatus.ANSWERED)
                .build();
        when(examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, questionId1))
                .thenReturn(List.of(turn1));

        ExamAttemptQuestionSnapshot snap1 = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId1, UUID.randomUUID(), 0, "Q1", "A1", "[]"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap1));

        ExamScore score = ExamScore.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .build();
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.of(score));

        User student = User.builder()
                .userId(studentId)
                .fullName("Student")
                .email("student@example.com")
                .build();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        GradeQuestionCommand cmd = new GradeQuestionCommand(new BigDecimal("9.50"), "Admin override grade");
        AttemptGradingDetailView result = service.gradeQuestion(attemptId, questionId1, cmd, adminId);

        assertThat(result).isNotNull();
        verify(examTurnRepository).save(argThat(t -> new BigDecimal("9.50").compareTo(t.getTeacherScore()) == 0));
        verify(examAttemptRepository).save(argThat(a -> new BigDecimal("9.50").compareTo(a.getTotalScore()) == 0));
    }

    @Test
    @DisplayName("overrideScore: sets overrideTotalScore, overrideReason, and updates attempt score")
    void overrideScore_Success() {
        UUID scoreId = UUID.randomUUID();
        ExamScore score = ExamScore.builder()
                .id(scoreId)
                .attemptId(attemptId)
                .totalScore(new BigDecimal("6.00"))
                .build();
        when(examScoreRepository.findById(scoreId)).thenReturn(Optional.of(score));

        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .subjectId(subjectId)
                .examId(examId)
                .studentId(studentId)
                .status(AttemptStatus.COMPLETED)
                .totalScore(new BigDecimal("6.00"))
                .build();
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(lecturerId)
                .build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        when(examScoreRepository.save(any(ExamScore.class))).thenAnswer(inv -> inv.getArgument(0));

        OverrideScoreCommand cmd = new OverrideScoreCommand(new BigDecimal("8.50"), "Discretionary bonus");
        LecturerScoreView view = service.overrideScore(scoreId, cmd, lecturerId);

        assertThat(view).isNotNull();
        assertThat(view.overrideTotalScore()).isEqualByComparingTo("8.50");
        verify(examScoreRepository).save(argThat(s -> "Discretionary bonus".equals(s.getOverrideReason())));
        verify(examAttemptRepository).save(argThat(a -> new BigDecimal("8.50").compareTo(a.getTotalScore()) == 0
                && a.getGradeLevel() == GradeLevel.EXCELLENT));
    }

    @Test
    @DisplayName("getExamStats: calculates participant counts and average score accurately")
    void getExamStats_Success() {
        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(lecturerId)
                .build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        ExamAttempt a1 = ExamAttempt.builder()
                .id(UUID.randomUUID())
                .examId(examId)
                .status(AttemptStatus.COMPLETED)
                .totalScore(new BigDecimal("8.00"))
                .build();
        ExamAttempt a2 = ExamAttempt.builder()
                .id(UUID.randomUUID())
                .examId(examId)
                .status(AttemptStatus.COMPLETED)
                .totalScore(new BigDecimal("6.00"))
                .build();
        ExamAttempt a3 = ExamAttempt.builder()
                .id(UUID.randomUUID())
                .examId(examId)
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        when(examAttemptRepository.findByExamId(examId)).thenReturn(List.of(a1, a2, a3));

        ExamGradingStatsView stats = service.getExamStats(examId, lecturerId);

        assertThat(stats).isNotNull();
        assertThat(stats.examId()).isEqualTo(examId);
        assertThat(stats.totalParticipants()).isEqualTo(3);
        assertThat(stats.submittedCount()).isEqualTo(2);
        assertThat(stats.pendingCount()).isEqualTo(1);
        assertThat(stats.averageScore()).isEqualByComparingTo("7.00");
    }

    @Test
    @DisplayName("getCourseStats: calculates aggregated metrics across exams in course")
    void getCourseStats_Success() {
        Exam exam1 = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(lecturerId)
                .build();
        when(examRepository.findBySubjectId(subjectId)).thenReturn(List.of(exam1));

        ExamAttempt a1 = ExamAttempt.builder()
                .id(UUID.randomUUID())
                .examId(examId)
                .status(AttemptStatus.COMPLETED)
                .totalScore(new BigDecimal("8.50"))
                .build();
        when(examAttemptRepository.findByExamId(examId)).thenReturn(List.of(a1));

        CourseGradingStatsView stats = service.getCourseStats(subjectId, lecturerId);

        assertThat(stats).isNotNull();
        assertThat(stats.courseId()).isEqualTo(subjectId);
        assertThat(stats.totalExams()).isEqualTo(1);
        assertThat(stats.totalParticipants()).isEqualTo(1);
        assertThat(stats.submittedCount()).isEqualTo(1);
        assertThat(stats.pendingCount()).isEqualTo(0);
        assertThat(stats.averageScore()).isEqualByComparingTo("8.50");
    }

    @Test
    @DisplayName("overrideScore: blank or null reason throws INVALID_REQUEST without mutating attempt or score")
    void overrideScore_BlankOrNullReason_ThrowsInvalidRequestWithNoMutation() {
        UUID scoreId = UUID.randomUUID();

        // Null reason
        OverrideScoreCommand cmdNull = new OverrideScoreCommand(new BigDecimal("9.00"), null);
        assertThatThrownBy(() -> service.overrideScore(scoreId, cmdNull, lecturerId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessageContaining("Override reason must not be blank");

        // Blank reason
        OverrideScoreCommand cmdBlank = new OverrideScoreCommand(new BigDecimal("9.00"), "   ");
        assertThatThrownBy(() -> service.overrideScore(scoreId, cmdBlank, lecturerId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST)
                .hasMessageContaining("Override reason must not be blank");

        // Verify zero mutations
        verifyNoInteractions(examAttemptRepository, examScoreRepository, auditor);
    }

    @Test
    @DisplayName("overrideScore: records audit event with old/new reason/score, preserves AI score/feedback untouched")
    void overrideScore_RecordsAuditEvent_AndPreservesAiScore() {
        UUID scoreId = UUID.randomUUID();
        BigDecimal originalAiScore = new BigDecimal("7.00");
        String originalAiFeedback = "AI generated detailed feedback";

        ExamScore score = ExamScore.builder()
                .id(scoreId)
                .attemptId(attemptId)
                .totalScore(new BigDecimal("7.00"))
                .aiScore(originalAiScore)
                .aiFeedback(originalAiFeedback)
                .overrideTotalScore(new BigDecimal("8.00"))
                .overrideReason("Old discretionary reason")
                .overrideBy(lecturerId)
                .build();
        when(examScoreRepository.findById(scoreId)).thenReturn(Optional.of(score));

        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .subjectId(subjectId)
                .examId(examId)
                .studentId(studentId)
                .status(AttemptStatus.COMPLETED)
                .totalScore(new BigDecimal("8.00"))
                .build();
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(lecturerId)
                .build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examScoreRepository.save(any(ExamScore.class))).thenAnswer(inv -> inv.getArgument(0));

        OverrideScoreCommand cmd = new OverrideScoreCommand(new BigDecimal("9.50"), "Oral defense bonus");
        LecturerScoreView view = service.overrideScore(scoreId, cmd, lecturerId);

        assertThat(view).isNotNull();
        assertThat(view.overrideTotalScore()).isEqualByComparingTo("9.50");

        // Verify audit event recorded
        ArgumentCaptor<Map<String, ?>> oldCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, ?>> newCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditor).record(eq(lecturerId), eq("SCORE_OVERRIDDEN"), eq("EXAM_SCORE"), eq(scoreId),
                oldCaptor.capture(), newCaptor.capture());

        Map<String, ?> oldVals = oldCaptor.getValue();
        assertThat(oldVals.get("overrideTotalScore")).isEqualTo(new BigDecimal("8.00"));
        assertThat(oldVals.get("overrideReason")).isEqualTo("Old discretionary reason");

        Map<String, ?> newVals = newCaptor.getValue();
        assertThat(newVals.get("overrideTotalScore")).isEqualTo(new BigDecimal("9.50"));
        assertThat(newVals.get("overrideReason")).isEqualTo("Oral defense bonus");
        assertThat(newVals.get("actor")).isEqualTo(lecturerId);
        assertThat(newVals.get("attemptId")).isEqualTo(attemptId);
        assertThat(newVals.get("timestamp")).isNotNull();

        // Verify AI score and feedback remain completely untouched
        assertThat(score.getAiScore()).isEqualByComparingTo(originalAiScore);
        assertThat(score.getAiFeedback()).isEqualTo(originalAiFeedback);

        // Verify effective total consistency across attempt and score
        assertThat(score.getEffectiveTotalScore()).isEqualByComparingTo("9.50");
        verify(examAttemptRepository).save(argThat(a -> new BigDecimal("9.50").compareTo(a.getTotalScore()) == 0));
    }

    @Test
    @DisplayName("gradeQuestion: regrading after score override clears override while retaining audit history")
    void gradeQuestion_AfterOverride_ClearsOverrideWhileRetainingAuditHistory() {
        ExamAttempt attempt = ExamAttempt.builder()
                .id(attemptId)
                .examId(examId)
                .subjectId(subjectId)
                .studentId(studentId)
                .status(AttemptStatus.COMPLETED)
                .totalQuestions(1)
                .totalScore(new BigDecimal("9.00")) // was overridden to 9.00
                .build();
        when(examAttemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        Exam exam = Exam.builder()
                .id(examId)
                .subjectId(subjectId)
                .creatorId(lecturerId)
                .build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        ExamTurn turn1 = ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId1)
                .turnType(TurnType.MAIN_QUESTION)
                .turnOrder(1)
                .status(TurnStatus.EVALUATED)
                .score(new BigDecimal("7.00"))
                .teacherScore(new BigDecimal("7.50"))
                .build();
        when(examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, questionId1))
                .thenReturn(List.of(turn1));

        ExamAttemptQuestionSnapshot snap1 = ExamAttemptQuestionSnapshot.create(
                attemptId, questionId1, UUID.randomUUID(), 0, "Q1", "A1", "[]"
        );
        when(snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)).thenReturn(List.of(snap1));

        UUID scoreId = UUID.randomUUID();
        ExamScore existingScore = ExamScore.builder()
                .id(scoreId)
                .attemptId(attemptId)
                .totalScore(new BigDecimal("7.50"))
                .aiScore(new BigDecimal("7.00"))
                .aiFeedback("AI original")
                .teacherScore(new BigDecimal("7.50"))
                .overrideTotalScore(new BigDecimal("9.00"))
                .overrideReason("Discretionary adjustment")
                .overrideBy(lecturerId)
                .build();
        when(examScoreRepository.findByAttemptId(attemptId)).thenReturn(Optional.of(existingScore));

        User student = User.builder()
                .userId(studentId)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .build();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        GradeQuestionCommand cmd = new GradeQuestionCommand(new BigDecimal("8.00"), "Regraded question 1");
        AttemptGradingDetailView result = service.gradeQuestion(attemptId, questionId1, cmd, lecturerId);

        assertThat(result).isNotNull();

        // 1. Verify question grade audit recorded
        verify(auditor).record(eq(lecturerId), eq("QUESTION_GRADED"), eq("EXAM_TURN"), eq(turn1.getId()),
                argThat(m -> new BigDecimal("7.50").equals(m.get("teacherScore"))),
                argThat(m -> new BigDecimal("8.00").equals(m.get("teacherScore")) && attemptId.equals(m.get("attemptId"))));

        // 2. Verify override cleared audit recorded
        verify(auditor).record(eq(lecturerId), eq("OVERRIDE_CLEARED"), eq("EXAM_SCORE"), eq(scoreId),
                argThat(m -> new BigDecimal("9.00").equals(m.get("overrideTotalScore")) && "Discretionary adjustment".equals(m.get("overrideReason"))),
                argThat(m -> "Cleared due to question regrade".equals(m.get("clearedReason")) && questionId1.equals(m.get("regradedQuestionId"))));

        // 3. Verify override fields cleared on saved score
        ArgumentCaptor<ExamScore> scoreCaptor = ArgumentCaptor.forClass(ExamScore.class);
        verify(examScoreRepository).save(scoreCaptor.capture());
        ExamScore savedScore = scoreCaptor.getValue();
        assertThat(savedScore.getOverrideTotalScore()).isNull();
        assertThat(savedScore.getOverrideReason()).isNull();
        assertThat(savedScore.getOverrideBy()).isNull();
        assertThat(savedScore.getOverrideAt()).isNull();

        // 4. Verify AI score and feedback remain untouched
        assertThat(savedScore.getAiScore()).isEqualByComparingTo("7.00");
        assertThat(savedScore.getAiFeedback()).isEqualTo("AI original");

        // 5. Verify recalculated effective total
        assertThat(savedScore.getTotalScore()).isEqualByComparingTo("8.00");
        assertThat(savedScore.getEffectiveTotalScore()).isEqualByComparingTo("8.00");
    }
}
