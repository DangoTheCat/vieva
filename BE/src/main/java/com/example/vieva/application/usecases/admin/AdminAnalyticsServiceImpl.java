package com.example.vieva.application.usecases.admin;

import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.common.UserSearchCriteria;
import com.example.vieva.application.ports.input.admin.AdminAnalyticsService;
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
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final SubjectRepository subjectRepository;

    @Override
    public AdminStatsView getStats() {
        long totalUsers = userRepository.findAll(UserSearchCriteria.builder().size(1).build()).getTotalElements();
        long studentCount = userRepository.findAll(UserSearchCriteria.builder().role("STUDENT").size(1).build()).getTotalElements();
        long lecturerCount = userRepository.findAll(UserSearchCriteria.builder().role("LECTURER").size(1).build()).getTotalElements();
        long adminCount = userRepository.findAll(UserSearchCriteria.builder().role("ADMIN").size(1).build()).getTotalElements();

        long totalExams = examRepository.count();
        long draftExams = examRepository.countByStatus(ExamStatus.DRAFT);
        long inProgressExams = examRepository.countByStatus(ExamStatus.IN_PROGRESS);
        long completedExams = examRepository.countByStatus(ExamStatus.COMPLETED);

        long totalAttempts = examAttemptRepository.count();
        long readyAttempts = examAttemptRepository.countByStatus(AttemptStatus.READY);
        long inProgressAttempts = examAttemptRepository.countByStatus(AttemptStatus.IN_PROGRESS);
        long reconnectingAttempts = examAttemptRepository.countByStatus(AttemptStatus.RECONNECTING);
        long finalizingAttempts = examAttemptRepository.countByStatus(AttemptStatus.FINALIZING);
        long completedAttempts = examAttemptRepository.countByStatus(AttemptStatus.COMPLETED);
        long interruptedAttempts = examAttemptRepository.countByStatus(AttemptStatus.INTERRUPTED);

        return new AdminStatsView(
                totalUsers,
                studentCount,
                lecturerCount,
                adminCount,
                totalExams,
                draftExams,
                inProgressExams,
                completedExams,
                totalAttempts,
                readyAttempts,
                inProgressAttempts,
                reconnectingAttempts,
                finalizingAttempts,
                completedAttempts,
                interruptedAttempts
        );
    }

    @Override
    public PageResult<AdminPerformanceItemView> getPerformance(int page, int size) {
        PageResult<ExamAttempt> attemptsPage = examAttemptRepository.findCompletedAttempts(page, size);
        return toPerformanceView(attemptsPage);
    }

    @Override
    public PageResult<AdminPerformanceItemView> getPerformance(
            UUID studentId,
            UUID subjectId,
            UUID examId,
            AttemptStatus status,
            int page,
            int size
    ) {
        if (studentId == null && subjectId == null && examId == null && status == null) {
            return getPerformance(page, size);
        }
        PageResult<ExamAttempt> attemptsPage = examAttemptRepository.findAttemptsWithFilters(
                studentId, subjectId, examId, status, page, size
        );
        return toPerformanceView(attemptsPage);
    }

    private PageResult<AdminPerformanceItemView> toPerformanceView(PageResult<ExamAttempt> attemptsPage) {
        List<AdminPerformanceItemView> items = attemptsPage.getContent().stream()
                .map(a -> {
                    User student = userRepository.findById(a.getStudentId()).orElse(null);
                    String studentName = student != null ? student.getFullName() : "Unknown";
                    String studentEmail = student != null ? student.getEmail() : "";

                    String examTitle = "Exam";
                    if (a.getExamId() != null) {
                        examTitle = examRepository.findById(a.getExamId())
                                .map(Exam::getTitle)
                                .orElse("Exam");
                    }

                    String subjectName = "Subject";
                    if (a.getSubjectId() != null) {
                        subjectName = subjectRepository.findById(a.getSubjectId())
                                .map(Subject::getSubjectName)
                                .orElse("Subject");
                    }

                    return new AdminPerformanceItemView(
                            a.getId(),
                            studentName,
                            studentEmail,
                            examTitle,
                            subjectName,
                            a.getTotalScore(),
                            a.getGradeLevel(),
                            a.getCompletedAt()
                    );
                })
                .collect(Collectors.toList());

        return PageResult.of(items, attemptsPage.getPage(), attemptsPage.getSize(), attemptsPage.getTotalElements());
    }
}
