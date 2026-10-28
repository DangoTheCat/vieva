package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.*;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.*;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StudentExamServiceImpl implements StudentExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAttemptQuestionSnapshotRepository snapshotRepository;
    private final ExamTurnRepository examTurnRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final JsonSerializerPort jsonSerializerPort;
    private final SubjectRepository subjectRepository;

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<StudentExamView> listActiveExams(UUID studentId) {
        List<Exam> activeExams = examRepository.findByStatus(ExamStatus.IN_PROGRESS);
        return activeExams.stream()
                .map(exam -> {
                    int questionCount = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId()).size();
                    Optional<ExamAttempt> attemptOpt = examAttemptRepository.findByExamIdAndStudentId(exam.getId(), studentId);
                    AttemptStatus attemptStatus = attemptOpt.map(ExamAttempt::getStatus).orElse(null);
                    UUID attemptId = attemptOpt.map(ExamAttempt::getId).orElse(null);
                    return new StudentExamView(
                            exam.getId(),
                            exam.getSubjectId(),
                            exam.getTitle(),
                            exam.getDurationSeconds(),
                            questionCount,
                            attemptStatus,
                            attemptId
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @UseCaseTransaction
    public ExamAttemptView enterExam(UUID examId, UUID studentId) {
        if (examId == null || studentId == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam ID and Student ID are required");
        }

        // 1. Lock exam
        Exam exam = examRepository.findByIdForUpdate(examId)
                .or(() -> examRepository.findById(examId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXAM_NOT_FOUND));

        if (exam.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new ApplicationException(ErrorCode.EXAM_CLOSED, "Exam is closed for new attempts");
        }

        // 2. Recheck existing attempt under lock
        Optional<ExamAttempt> existing = examAttemptRepository.findByExamIdAndStudentId(examId, studentId);
        if (existing.isPresent()) {
            return toAttemptView(existing.get());
        }

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId);
        if (examQuestions.isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam has no questions configured");
        }

        // 3. Atomically create attempt
        ExamAttempt attempt = ExamAttempt.initForExam(
                examId,
                studentId,
                exam.getSubjectId(),
                examQuestions.size(),
                exam.getDurationSeconds()
        );
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        // 4. Atomically create snapshots
        List<ExamAttemptQuestionSnapshot> snapshots = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            QuestionVersion version = questionVersionRepository.findById(eq.getQuestionVersionId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_VERSION_NOT_FOUND));

            Optional<Rubric> rubricOpt = rubricRepository.findByQuestionVersionId(eq.getQuestionVersionId());
            String rubricJson = "[]";
            if (rubricOpt.isPresent()) {
                List<RubricCriterion> criteria = rubricCriterionRepository.findByRubricId(rubricOpt.get().getRubricId());
                rubricJson = jsonSerializerPort.serialize(criteria);
            }

            ExamAttemptQuestionSnapshot snapshot = ExamAttemptQuestionSnapshot.create(
                    savedAttempt.getId(),
                    eq.getQuestionId(),
                    eq.getQuestionVersionId(),
                    eq.getOrderIndex(),
                    version.getQuestionContent(),
                    version.getReferenceAnswer(),
                    rubricJson
            );
            snapshots.add(snapshot);
        }
        snapshotRepository.saveAll(snapshots);

        // 5. Pre-create the first turn for the first frozen question
        ExamAttemptQuestionSnapshot firstSnapshot = snapshots.get(0);
        ExamTurn firstTurn = ExamTurn.createMainQuestion(
                savedAttempt.getId(),
                firstSnapshot.getQuestionId(),
                firstSnapshot.getQuestionVersionId(),
                1,
                firstSnapshot.getQuestionText(),
                null
        );
        examTurnRepository.save(firstTurn);

        return toAttemptView(savedAttempt);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<StudentFrozenQuestionView> getAttemptQuestions(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = getOwnedAttempt(attemptId, studentId);
        List<ExamAttemptQuestionSnapshot> snapshots = snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attempt.getId());
        return snapshots.stream()
                .map(s -> new StudentFrozenQuestionView(
                        s.getQuestionId(),
                        s.getQuestionVersionId(),
                        s.getOrderIndex(),
                        s.getQuestionText()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<ExamTurnDetailView> getAttemptTurns(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = getOwnedAttempt(attemptId, studentId);
        List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attempt.getId());
        return turns.stream()
                .map(t -> new ExamTurnDetailView(
                        t.getId(),
                        t.getAttemptId(),
                        t.getQuestionId(),
                        t.getQuestionVersionId(),
                        t.getTurnType().name(),
                        t.getParentTurnId(),
                        t.getTurnOrder(),
                        t.getQuestionText(),
                        t.getTtsAudioUrl(),
                        t.getStudentAudioUrl(),
                        t.getTranscriptText(),
                        t.getStatus().name(),
                        t.getScore(),
                        t.getFeedback(),
                        t.getRubricEvaluations(),
                        t.getCreatedAt(),
                        t.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<StudentScoreView> getStudentScores(UUID studentId) {
        if (studentId == null) {
            return List.of();
        }
        List<ExamAttempt> attempts = examAttemptRepository.findByStudentId(studentId);
        return attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED)
                .map(a -> {
                    String title = "Exam";
                    if (a.getExamId() != null) {
                        title = examRepository.findById(a.getExamId())
                                .map(Exam::getTitle)
                                .orElse("Exam");
                    } else if (a.getSubjectId() != null) {
                        title = subjectRepository.findById(a.getSubjectId())
                                .map(Subject::getSubjectName)
                                .orElse("Exam");
                    }
                    return new StudentScoreView(
                            a.getId(),
                            a.getExamId(),
                            title,
                            a.getTotalScore(),
                            a.getGradeLevel(),
                            a.getCompletedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    private ExamAttempt getOwnedAttempt(UUID attemptId, UUID studentId) {
        if (attemptId == null) {
            throw new ApplicationException(ErrorCode.ATTEMPT_NOT_FOUND);
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ATTEMPT_NOT_FOUND));
        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApplicationException(ErrorCode.ATTEMPT_NOT_OWNED);
        }
        return attempt;
    }

    private ExamAttemptView toAttemptView(ExamAttempt attempt) {
        List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attempt.getId());
        ExamTurn lastTurn = turns.isEmpty() ? null : turns.get(turns.size() - 1);
        return new ExamAttemptView(
                attempt.getId(),
                attempt.getStudentId(),
                attempt.getSubjectId(),
                attempt.getStatus().name(),
                attempt.getCurrentQuestionIndex(),
                attempt.getTotalQuestions(),
                attempt.getTotalScore(),
                attempt.getGradeLevel() != null ? attempt.getGradeLevel().name() : null,
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getPausedAt(),
                attempt.remainingSecondsAt(Instant.now()),
                attempt.getGraceDeadline(),
                lastTurn != null ? lastTurn.getId() : null,
                lastTurn != null ? lastTurn.getQuestionText() : null,
                lastTurn != null ? lastTurn.getTtsAudioUrl() : null
        );
    }
}
