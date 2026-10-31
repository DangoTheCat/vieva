package com.example.vieva.application.usecases.grading;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.ExamTurnDetailView;
import com.example.vieva.application.ports.input.exam.ExamView;
import com.example.vieva.application.ports.input.grading.*;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class LecturerGradingServiceImpl implements LecturerGradingService {

    private final SubjectRepository subjectRepository;
    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAttemptQuestionSnapshotRepository snapshotRepository;
    private final ExamTurnRepository examTurnRepository;
    private final ExamScoreRepository examScoreRepository;
    private final UserRepository userRepository;
    private final SubjectAccessGuard accessGuard;
    private final QuestionBankAuditor auditor;

    public LecturerGradingServiceImpl(
            SubjectRepository subjectRepository,
            LecturerSubjectRepository lecturerSubjectRepository,
            ExamRepository examRepository,
            ExamQuestionRepository examQuestionRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            ExamTurnRepository examTurnRepository,
            ExamScoreRepository examScoreRepository,
            UserRepository userRepository,
            SubjectAccessGuard accessGuard,
            QuestionBankAuditor auditor
    ) {
        this.subjectRepository = subjectRepository;
        this.lecturerSubjectRepository = lecturerSubjectRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.snapshotRepository = snapshotRepository;
        this.examTurnRepository = examTurnRepository;
        this.examScoreRepository = examScoreRepository;
        this.userRepository = userRepository;
        this.accessGuard = accessGuard;
        this.auditor = auditor;
    }

    public LecturerGradingServiceImpl(
            SubjectRepository subjectRepository,
            LecturerSubjectRepository lecturerSubjectRepository,
            ExamRepository examRepository,
            ExamQuestionRepository examQuestionRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            ExamTurnRepository examTurnRepository,
            ExamScoreRepository examScoreRepository,
            UserRepository userRepository,
            SubjectAccessGuard accessGuard
    ) {
        this(
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
                null
        );
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<GradingCourseView> getCoursesForGrading(UUID lecturerId) {
        boolean admin = isAdmin(lecturerId);
        List<Subject> subjects = new ArrayList<>();

        if (admin) {
            subjects = subjectRepository.findByStatus(SubjectStatus.ACTIVE);
        } else {
            List<LecturerSubject> assignments = lecturerSubjectRepository.findActiveByLecturerId(lecturerId);
            for (LecturerSubject ls : assignments) {
                subjectRepository.findById(ls.getSubjectId()).ifPresent(subjects::add);
            }
        }

        List<GradingCourseView> results = new ArrayList<>();
        for (Subject s : subjects) {
            List<Exam> exams = examRepository.findBySubjectId(s.getSubjectId());
            List<Exam> lecturerExams = exams.stream()
                    .filter(e -> admin || e.getCreatorId().equals(lecturerId))
                    .toList();

            long totalParticipants = 0;
            long submittedCount = 0;
            long pendingCount = 0;
            BigDecimal totalScoreSum = BigDecimal.ZERO;
            long scoredAttemptsCount = 0;

            for (Exam e : lecturerExams) {
                List<ExamAttempt> attempts = examAttemptRepository.findByExamId(e.getId());
                totalParticipants += attempts.size();
                for (ExamAttempt a : attempts) {
                    if (a.getStatus() == AttemptStatus.COMPLETED) {
                        submittedCount++;
                        if (a.getTotalScore() != null) {
                            totalScoreSum = totalScoreSum.add(a.getTotalScore());
                            scoredAttemptsCount++;
                        }
                    } else if (a.getStatus() == AttemptStatus.IN_PROGRESS
                            || a.getStatus() == AttemptStatus.FINALIZING
                            || a.getStatus() == AttemptStatus.READY
                            || a.getStatus() == AttemptStatus.RECONNECTING) {
                        pendingCount++;
                    }
                }
            }

            BigDecimal avgScore = scoredAttemptsCount > 0
                    ? totalScoreSum.divide(BigDecimal.valueOf(scoredAttemptsCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            results.add(new GradingCourseView(
                    s.getSubjectId(),
                    s.getSubjectCode(),
                    s.getSubjectName(),
                    lecturerExams.size(),
                    submittedCount,
                    pendingCount,
                    totalParticipants,
                    avgScore
            ));
        }
        return results;
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<ExamView> getExamsForCourse(UUID courseId, UUID lecturerId) {
        accessGuard.requireManage(lecturerId, courseId);
        boolean admin = isAdmin(lecturerId);
        List<Exam> exams = examRepository.findBySubjectId(courseId);
        return exams.stream()
                .filter(e -> admin || e.getCreatorId().equals(lecturerId))
                .map(e -> {
                    int count = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(e.getId()).size();
                    return new ExamView(
                            e.getId(),
                            e.getSubjectId(),
                            e.getCreatorId(),
                            e.getTitle(),
                            e.getDurationSeconds(),
                            e.getStatus().name(),
                            count,
                            e.getCreatedAt(),
                            e.getUpdatedAt(),
                            e.getAnswerTimeLimitSeconds(),
                            e.getMaxFollowUpsPerQuestion()
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<ExamParticipantView> getExamParticipants(UUID examId, UUID lecturerId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXAM_NOT_FOUND));
        checkExamAccess(exam, lecturerId);

        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId);
        List<ExamParticipantView> views = new ArrayList<>();
        for (ExamAttempt a : attempts) {
            User student = userRepository.findById(a.getStudentId()).orElse(null);
            String name = student != null ? student.getFullName() : "Unknown";
            String email = student != null ? student.getEmail() : "";
            String code = student != null ? student.getUserCode() : "";

            Optional<ExamScore> scoreOpt = examScoreRepository.findByAttemptId(a.getId());
            BigDecimal aiScore = scoreOpt.map(ExamScore::getAiScore).orElse(null);
            BigDecimal teacherScore = scoreOpt.map(ExamScore::getTeacherScore).orElse(null);
            BigDecimal overrideScore = scoreOpt.map(ExamScore::getOverrideTotalScore).orElse(null);

            views.add(new ExamParticipantView(
                    a.getId(),
                    a.getStudentId(),
                    name,
                    email,
                    code,
                    a.getStatus().name(),
                    a.getTotalScore(),
                    aiScore,
                    teacherScore,
                    overrideScore,
                    a.getStartedAt(),
                    a.getCompletedAt()
            ));
        }
        return views;
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public AttemptGradingDetailView getAttemptDetail(UUID attemptId, UUID lecturerId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ATTEMPT_NOT_FOUND));
        checkAttemptAccess(attempt, lecturerId);

        User student = userRepository.findById(attempt.getStudentId()).orElse(null);
        String name = student != null ? student.getFullName() : "Unknown";
        String email = student != null ? student.getEmail() : "";

        String examTitle = "Exam";
        if (attempt.getExamId() != null) {
            examTitle = examRepository.findById(attempt.getExamId())
                    .map(Exam::getTitle)
                    .orElse("Exam");
        }

        Optional<ExamScore> scoreOpt = examScoreRepository.findByAttemptId(attemptId);
        BigDecimal aiScore = scoreOpt.map(ExamScore::getAiScore).orElse(null);
        BigDecimal teacherScore = scoreOpt.map(ExamScore::getTeacherScore).orElse(null);
        BigDecimal overrideScore = scoreOpt.map(ExamScore::getOverrideTotalScore).orElse(null);

        List<ExamAttemptQuestionSnapshot> snapshots = snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId);
        List<QuestionGradingView> questionViews = new ArrayList<>();

        for (ExamAttemptQuestionSnapshot s : snapshots) {
            List<ExamTurn> turns = examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, s.getQuestionId());
            BigDecimal effectiveScore = BigDecimal.ZERO;
            BigDecimal qTeacherScore = null;
            String qTeacherFeedback = null;

            Optional<ExamTurn> teacherTurn = turns.stream().filter(t -> t.getTeacherScore() != null).findFirst();
            if (teacherTurn.isPresent()) {
                qTeacherScore = teacherTurn.get().getTeacherScore();
                qTeacherFeedback = teacherTurn.get().getTeacherFeedback();
                effectiveScore = qTeacherScore;
            } else {
                List<ExamTurn> evaluated = turns.stream()
                        .filter(t -> t.getStatus() == TurnStatus.EVALUATED && t.getScore() != null)
                        .toList();
                if (!evaluated.isEmpty()) {
                    effectiveScore = evaluated.get(evaluated.size() - 1).getScore();
                }
            }

            List<ExamTurnDetailView> turnViews = turns.stream()
                    .map(t -> new ExamTurnDetailView(
                            t.getId(),
                            t.getAttemptId(),
                            t.getQuestionId(),
                            t.getQuestionVersionId(),
                            t.getTurnType() != null ? t.getTurnType().name() : "MAIN_QUESTION",
                            t.getParentTurnId(),
                            t.getTurnOrder() != null ? t.getTurnOrder() : 0,
                            t.getQuestionText(),
                            t.getTtsAudioUrl(),
                            t.getStudentAudioUrl(),
                            t.getTranscriptText(),
                            t.getStatus() != null ? t.getStatus().name() : "PENDING",
                            t.getScore(),
                            t.getFeedback(),
                            t.getRubricEvaluations(),
                            t.getCreatedAt(),
                            t.getUpdatedAt()
                    ))
                    .collect(Collectors.toList());

            questionViews.add(new QuestionGradingView(
                    s.getQuestionId(),
                    s.getQuestionVersionId(),
                    s.getOrderIndex(),
                    s.getQuestionText(),
                    s.getExpectedAnswer(),
                    effectiveScore,
                    qTeacherScore,
                    qTeacherFeedback,
                    turnViews
            ));
        }

        return new AttemptGradingDetailView(
                attempt.getId(),
                attempt.getExamId(),
                examTitle,
                attempt.getStudentId(),
                name,
                email,
                attempt.getStatus().name(),
                attempt.getTotalScore(),
                aiScore,
                teacherScore,
                overrideScore,
                questionViews
        );
    }

    @Override
    @UseCaseTransaction
    public AttemptGradingDetailView gradeQuestion(UUID attemptId, UUID questionId, GradeQuestionCommand command, UUID lecturerId) {
        if (command == null || command.score() == null
                || !Double.isFinite(command.score().doubleValue())
                || command.score().compareTo(BigDecimal.ZERO) < 0
                || command.score().compareTo(BigDecimal.TEN) > 0) {
            throw new ApplicationException(ErrorCode.INVALID_SCORE, "Score must be between 0 and 10");
        }

        ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ATTEMPT_NOT_FOUND));
        checkAttemptAccess(attempt, lecturerId);

        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Grading is only permitted on completed attempts");
        }

        List<ExamAttemptQuestionSnapshot> snapshots = snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId);
        boolean isSnapshotted = !snapshots.isEmpty();

        if (isSnapshotted) {
            boolean validQuestion = snapshots.stream().anyMatch(s -> s.getQuestionId().equals(questionId));
            if (!validQuestion) {
                throw new ApplicationException(ErrorCode.QUESTION_NOT_FOUND, "Question not found in attempt snapshot");
            }
        } else {
            boolean validInTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId).stream()
                    .anyMatch(t -> questionId.equals(t.getQuestionId()));
            if (!validInTurns) {
                throw new ApplicationException(ErrorCode.QUESTION_NOT_FOUND, "Question not found in attempt turns");
            }
        }

        List<ExamTurn> turns = examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, questionId);
        ExamTurn gradedTurn = null;
        BigDecimal oldTeacherScore = null;
        if (!turns.isEmpty()) {
            gradedTurn = turns.get(0);
            oldTeacherScore = gradedTurn.getTeacherScore();
            gradedTurn.gradeByTeacher(command.score(), command.feedback(), lecturerId);
            examTurnRepository.save(gradedTurn);
        } else if (isSnapshotted) {
            ExamAttemptQuestionSnapshot snap = snapshots.stream()
                    .filter(s -> s.getQuestionId().equals(questionId))
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_NOT_FOUND));
            List<ExamTurn> allTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
            gradedTurn = ExamTurn.createMainQuestion(
                    attemptId,
                    questionId,
                    snap.getQuestionVersionId(),
                    allTurns.size() + 1,
                    snap.getQuestionText(),
                    null
            );
            gradedTurn.gradeByTeacher(command.score(), command.feedback(), lecturerId);
            examTurnRepository.save(gradedTurn);
        }

        if (auditor != null) {
            Map<String, Object> oldValues = new LinkedHashMap<>();
            oldValues.put("teacherScore", oldTeacherScore);

            Map<String, Object> newValues = new LinkedHashMap<>();
            newValues.put("teacherScore", command.score());
            newValues.put("feedback", command.feedback());
            newValues.put("actor", lecturerId);
            newValues.put("attemptId", attemptId);
            newValues.put("turnId", gradedTurn != null ? gradedTurn.getId() : null);
            newValues.put("questionId", questionId);

            auditor.record(lecturerId, "QUESTION_GRADED", "EXAM_TURN",
                    gradedTurn != null ? gradedTurn.getId() : questionId, oldValues, newValues);
        }

        // Aggregate score by main question groups:
        // Teacher score takes precedence, otherwise last evaluated main/followup score; unanswered questions get 0.
        BigDecimal sum = BigDecimal.ZERO;
        int totalQuestions = isSnapshotted ? snapshots.size() : Math.max(1, attempt.getTotalQuestions());

        if (isSnapshotted) {
            for (ExamAttemptQuestionSnapshot s : snapshots) {
                List<ExamTurn> qTurns = examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, s.getQuestionId());
                BigDecimal effective = BigDecimal.ZERO;
                Optional<ExamTurn> teacherTurn = qTurns.stream().filter(t -> t.getTeacherScore() != null).findFirst();
                if (teacherTurn.isPresent()) {
                    effective = teacherTurn.get().getTeacherScore();
                } else {
                    List<ExamTurn> evaluated = qTurns.stream()
                            .filter(t -> t.getStatus() == TurnStatus.EVALUATED && t.getScore() != null)
                            .toList();
                    if (!evaluated.isEmpty()) {
                        effective = evaluated.get(evaluated.size() - 1).getScore();
                    }
                }
                sum = sum.add(effective);
            }
        } else {
            List<ExamTurn> allTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
            Map<UUID, List<ExamTurn>> grouped = allTurns.stream()
                    .filter(t -> t.getQuestionId() != null)
                    .collect(Collectors.groupingBy(ExamTurn::getQuestionId));
            for (List<ExamTurn> qTurns : grouped.values()) {
                BigDecimal effective = BigDecimal.ZERO;
                Optional<ExamTurn> teacherTurn = qTurns.stream().filter(t -> t.getTeacherScore() != null).findFirst();
                if (teacherTurn.isPresent()) {
                    effective = teacherTurn.get().getTeacherScore();
                } else {
                    List<ExamTurn> evaluated = qTurns.stream()
                            .filter(t -> t.getStatus() == TurnStatus.EVALUATED && t.getScore() != null)
                            .toList();
                    if (!evaluated.isEmpty()) {
                        effective = evaluated.get(evaluated.size() - 1).getScore();
                    }
                }
                sum = sum.add(effective);
            }
        }

        BigDecimal avgScore = sum.divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP);
        GradeLevel grade = determineGrade(avgScore);
        attempt.recordScore(avgScore, grade);
        examAttemptRepository.save(attempt);

        ExamScore score = examScoreRepository.findByAttemptId(attemptId)
                .orElseGet(() -> ExamScore.create(attemptId, avgScore, grade, "Teacher graded", ScoringMode.RULE_BASED_FALLBACK));

        if (score.getOverrideTotalScore() != null) {
            if (auditor != null) {
                Map<String, Object> oldValues = new LinkedHashMap<>();
                oldValues.put("overrideTotalScore", score.getOverrideTotalScore());
                oldValues.put("overrideReason", score.getOverrideReason());
                oldValues.put("overrideBy", score.getOverrideBy());
                oldValues.put("overrideAt", score.getOverrideAt() != null ? score.getOverrideAt().toString() : null);

                Map<String, Object> newValues = new LinkedHashMap<>();
                newValues.put("overrideTotalScore", null);
                newValues.put("overrideReason", null);
                newValues.put("clearedReason", "Cleared due to question regrade");
                newValues.put("regradedQuestionId", questionId);
                newValues.put("attemptId", attemptId);
                newValues.put("actor", lecturerId);

                auditor.record(lecturerId, "OVERRIDE_CLEARED", "EXAM_SCORE", score.getId(), oldValues, newValues);
            }
            score.clearOverride(); // Question regrading clears manual total override
        }

        score.recordTeacherGrade(avgScore, grade, command.feedback(), lecturerId);
        examScoreRepository.save(score);

        return getAttemptDetail(attemptId, lecturerId);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<LecturerScoreView> listScores(UUID lecturerId) {
        boolean admin = isAdmin(lecturerId);
        List<ExamScore> allScores = examScoreRepository.findAll();
        List<LecturerScoreView> views = new ArrayList<>();

        for (ExamScore s : allScores) {
            Optional<ExamAttempt> attemptOpt = examAttemptRepository.findById(s.getAttemptId());
            if (attemptOpt.isEmpty()) {
                continue;
            }
            ExamAttempt a = attemptOpt.get();

            if (!admin) {
                if (a.getExamId() != null) {
                    Optional<Exam> examOpt = examRepository.findById(a.getExamId());
                    if (examOpt.isEmpty() || !examOpt.get().getCreatorId().equals(lecturerId)
                            || !accessGuard.canManage(lecturerId, a.getSubjectId())) {
                        continue;
                    }
                } else if (!accessGuard.canManage(lecturerId, a.getSubjectId())) {
                    continue;
                }
            }

            String title = "Exam";
            if (a.getExamId() != null) {
                title = examRepository.findById(a.getExamId()).map(Exam::getTitle).orElse("Exam");
            }
            User student = userRepository.findById(a.getStudentId()).orElse(null);
            String studentName = student != null ? student.getFullName() : "Unknown";

            views.add(new LecturerScoreView(
                    s.getId(),
                    s.getAttemptId(),
                    a.getExamId(),
                    title,
                    a.getStudentId(),
                    studentName,
                    s.getOverrideTotalScore() != null ? s.getOverrideTotalScore() : s.getTotalScore(),
                    s.getOverrideTotalScore(),
                    s.getGrade() != null ? s.getGrade().name() : null,
                    s.getCreatedAt()
            ));
        }
        return views;
    }

    @Override
    @UseCaseTransaction
    public LecturerScoreView overrideScore(UUID scoreId, OverrideScoreCommand command, UUID lecturerId) {
        if (command == null || command.overrideTotalScore() == null
                || !Double.isFinite(command.overrideTotalScore().doubleValue())
                || command.overrideTotalScore().compareTo(BigDecimal.ZERO) < 0
                || command.overrideTotalScore().compareTo(BigDecimal.TEN) > 0) {
            throw new ApplicationException(ErrorCode.INVALID_SCORE, "Score must be between 0 and 10");
        }

        if (command.reason() == null || command.reason().isBlank()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Override reason must not be blank");
        }

        ExamScore score = examScoreRepository.findById(scoreId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Score not found"));

        ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(score.getAttemptId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ATTEMPT_NOT_FOUND));
        checkAttemptAccess(attempt, lecturerId);

        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Score override is only permitted on completed attempts");
        }

        BigDecimal oldOverrideScore = score.getOverrideTotalScore();
        String oldOverrideReason = score.getOverrideReason();

        score.overrideTotal(command.overrideTotalScore(), lecturerId, command.reason());
        GradeLevel grade = determineGrade(command.overrideTotalScore());
        score.applyGrade(grade);
        attempt.recordScore(command.overrideTotalScore(), grade);
        examAttemptRepository.save(attempt);
        ExamScore saved = examScoreRepository.save(score);

        if (auditor != null) {
            Map<String, Object> oldValues = new LinkedHashMap<>();
            oldValues.put("overrideTotalScore", oldOverrideScore);
            oldValues.put("overrideReason", oldOverrideReason);

            Map<String, Object> newValues = new LinkedHashMap<>();
            newValues.put("overrideTotalScore", command.overrideTotalScore());
            newValues.put("overrideReason", command.reason());
            newValues.put("actor", lecturerId);
            newValues.put("attemptId", attempt.getId());
            newValues.put("timestamp", Instant.now().toString());

            auditor.record(lecturerId, "SCORE_OVERRIDDEN", "EXAM_SCORE", score.getId(), oldValues, newValues);
        }

        String title = "Exam";
        if (attempt.getExamId() != null) {
            title = examRepository.findById(attempt.getExamId()).map(Exam::getTitle).orElse("Exam");
        }
        User student = userRepository.findById(attempt.getStudentId()).orElse(null);
        String studentName = student != null ? student.getFullName() : "Unknown";

        return new LecturerScoreView(
                saved.getId(),
                saved.getAttemptId(),
                attempt.getExamId(),
                title,
                attempt.getStudentId(),
                studentName,
                saved.getOverrideTotalScore() != null ? saved.getOverrideTotalScore() : saved.getTotalScore(),
                saved.getOverrideTotalScore(),
                saved.getGrade() != null ? saved.getGrade().name() : null,
                saved.getCreatedAt()
        );
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public ExamGradingStatsView getExamStats(UUID examId, UUID lecturerId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXAM_NOT_FOUND));
        checkExamAccess(exam, lecturerId);

        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId);
        long totalParticipants = attempts.size();
        long submittedCount = 0;
        long pendingCount = 0;
        BigDecimal totalScoreSum = BigDecimal.ZERO;
        long scoredCount = 0;

        for (ExamAttempt a : attempts) {
            if (a.getStatus() == AttemptStatus.COMPLETED) {
                submittedCount++;
                if (a.getTotalScore() != null) {
                    totalScoreSum = totalScoreSum.add(a.getTotalScore());
                    scoredCount++;
                }
            } else if (a.getStatus() == AttemptStatus.IN_PROGRESS
                    || a.getStatus() == AttemptStatus.FINALIZING
                    || a.getStatus() == AttemptStatus.READY
                    || a.getStatus() == AttemptStatus.RECONNECTING) {
                pendingCount++;
            }
        }

        BigDecimal averageScore = scoredCount > 0
                ? totalScoreSum.divide(BigDecimal.valueOf(scoredCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new ExamGradingStatsView(examId, totalParticipants, submittedCount, pendingCount, averageScore);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public CourseGradingStatsView getCourseStats(UUID courseId, UUID lecturerId) {
        accessGuard.requireManage(lecturerId, courseId);
        boolean admin = isAdmin(lecturerId);

        List<Exam> exams = examRepository.findBySubjectId(courseId).stream()
                .filter(e -> admin || e.getCreatorId().equals(lecturerId))
                .toList();

        long totalParticipants = 0;
        long submittedCount = 0;
        long pendingCount = 0;
        BigDecimal totalScoreSum = BigDecimal.ZERO;
        long scoredCount = 0;

        for (Exam e : exams) {
            List<ExamAttempt> attempts = examAttemptRepository.findByExamId(e.getId());
            totalParticipants += attempts.size();
            for (ExamAttempt a : attempts) {
                if (a.getStatus() == AttemptStatus.COMPLETED) {
                    submittedCount++;
                    if (a.getTotalScore() != null) {
                        totalScoreSum = totalScoreSum.add(a.getTotalScore());
                        scoredCount++;
                    }
                } else if (a.getStatus() == AttemptStatus.IN_PROGRESS
                        || a.getStatus() == AttemptStatus.FINALIZING
                        || a.getStatus() == AttemptStatus.READY
                        || a.getStatus() == AttemptStatus.RECONNECTING) {
                    pendingCount++;
                }
            }
        }

        BigDecimal averageScore = scoredCount > 0
                ? totalScoreSum.divide(BigDecimal.valueOf(scoredCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new CourseGradingStatsView(
                courseId,
                exams.size(),
                totalParticipants,
                submittedCount,
                pendingCount,
                averageScore
        );
    }

    private boolean isAdmin(UUID actorId) {
        if (actorId == null) {
            return false;
        }
        return userRepository.findById(actorId)
                .filter(u -> !u.isDeleted() && u.getStatus() == UserStatus.ACTIVE)
                .map(User::isAdmin)
                .orElse(false);
    }

    private void checkExamAccess(Exam exam, UUID lecturerId) {
        accessGuard.requireManage(lecturerId, exam.getSubjectId());
        if (!isAdmin(lecturerId) && !exam.getCreatorId().equals(lecturerId)) {
            throw new ApplicationException(ErrorCode.GRADING_FORBIDDEN, "Only the exam creator can access grading for this exam");
        }
    }

    private void checkAttemptAccess(ExamAttempt attempt, UUID lecturerId) {
        accessGuard.requireManage(lecturerId, attempt.getSubjectId());
        if (!isAdmin(lecturerId) && attempt.getExamId() != null) {
            Exam exam = examRepository.findById(attempt.getExamId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.EXAM_NOT_FOUND));
            if (!exam.getCreatorId().equals(lecturerId)) {
                throw new ApplicationException(ErrorCode.GRADING_FORBIDDEN, "Only the exam creator can access grading for this exam");
            }
        }
    }

    private GradeLevel determineGrade(BigDecimal score) {
        return GradeLevel.fromScore(score);
    }
}
