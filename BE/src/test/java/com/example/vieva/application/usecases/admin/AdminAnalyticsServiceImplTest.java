package com.example.vieva.application.usecases.admin;

import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.input.admin.AdminPerformanceItemView;
import com.example.vieva.application.ports.input.admin.AdminStatsView;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.Exam;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamStatus;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.User;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private SubjectRepository subjectRepository;

    private AdminAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminAnalyticsServiceImpl(
                userRepository,
                examRepository,
                examAttemptRepository,
                subjectRepository
        );
    }

    @Test
    @DisplayName("getStats: returns per-status counts for exams and attempts")
    void getStats_ReturnsCounts() {
        when(userRepository.findAll(any())).thenReturn(
                PageResult.<User>builder().totalElements(10L).build()
        );
        when(examRepository.count()).thenReturn(5L);
        when(examRepository.countByStatus(ExamStatus.DRAFT)).thenReturn(2L);
        when(examRepository.countByStatus(ExamStatus.IN_PROGRESS)).thenReturn(2L);
        when(examRepository.countByStatus(ExamStatus.COMPLETED)).thenReturn(1L);

        when(examAttemptRepository.count()).thenReturn(20L);
        when(examAttemptRepository.countByStatus(AttemptStatus.READY)).thenReturn(1L);
        when(examAttemptRepository.countByStatus(AttemptStatus.IN_PROGRESS)).thenReturn(2L);
        when(examAttemptRepository.countByStatus(AttemptStatus.RECONNECTING)).thenReturn(0L);
        when(examAttemptRepository.countByStatus(AttemptStatus.FINALIZING)).thenReturn(1L);
        when(examAttemptRepository.countByStatus(AttemptStatus.COMPLETED)).thenReturn(15L);
        when(examAttemptRepository.countByStatus(AttemptStatus.INTERRUPTED)).thenReturn(1L);

        AdminStatsView stats = service.getStats();

        assertThat(stats).isNotNull();
        assertThat(stats.totalExams()).isEqualTo(5L);
        assertThat(stats.draftExams()).isEqualTo(2L);
        assertThat(stats.inProgressExams()).isEqualTo(2L);
        assertThat(stats.completedExams()).isEqualTo(1L);

        assertThat(stats.totalAttempts()).isEqualTo(20L);
        assertThat(stats.readyAttempts()).isEqualTo(1L);
        assertThat(stats.inProgressAttempts()).isEqualTo(2L);
        assertThat(stats.completedAttempts()).isEqualTo(15L);
        assertThat(stats.interruptedAttempts()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getPerformance: returns paginated completed exam results")
    void getPerformance_ReturnsPage() {
        ExamAttempt attempt = ExamAttempt.builder()
                .id(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .totalScore(new BigDecimal("9.00"))
                .status(AttemptStatus.COMPLETED)
                .build();

        PageResult<ExamAttempt> pageResult = PageResult.<ExamAttempt>builder()
                .content(List.of(attempt))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();
        when(examAttemptRepository.findCompletedAttempts(0, 10)).thenReturn(pageResult);

        PageResult<AdminPerformanceItemView> performance = service.getPerformance(0, 10);

        assertThat(performance).isNotNull();
        assertThat(performance.getContent()).hasSize(1);
        assertThat(performance.getContent().get(0).totalScore()).isEqualByComparingTo("9.00");
    }

    @Test
    @DisplayName("getPerformance with filters: queries repository with given filters")
    void getPerformance_WithFilters_ReturnsFilteredPage() {
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        AttemptStatus status = AttemptStatus.COMPLETED;

        ExamAttempt attempt = ExamAttempt.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .subjectId(subjectId)
                .examId(examId)
                .totalScore(new BigDecimal("8.50"))
                .status(status)
                .build();

        PageResult<ExamAttempt> pageResult = PageResult.<ExamAttempt>builder()
                .content(List.of(attempt))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(examAttemptRepository.findAttemptsWithFilters(studentId, subjectId, examId, status, 0, 10))
                .thenReturn(pageResult);

        User student = User.builder().userId(studentId).fullName("Filtered Student").email("f@example.com").build();
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        Exam exam = Exam.builder().id(examId).title("Final Exam").build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        Subject subject = Subject.builder().subjectId(subjectId).subjectName("Computer Science").build();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        PageResult<AdminPerformanceItemView> performance = service.getPerformance(studentId, subjectId, examId, status, 0, 10);

        assertThat(performance).isNotNull();
        assertThat(performance.getContent()).hasSize(1);
        AdminPerformanceItemView item = performance.getContent().get(0);
        assertThat(item.studentName()).isEqualTo("Filtered Student");
        assertThat(item.examTitle()).isEqualTo("Final Exam");
        assertThat(item.subjectName()).isEqualTo("Computer Science");
        assertThat(item.totalScore()).isEqualByComparingTo("8.50");
    }
}
