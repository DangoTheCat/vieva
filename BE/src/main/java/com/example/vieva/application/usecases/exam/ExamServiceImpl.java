package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.*;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.*;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final SubjectAccessGuard accessGuard;
    private final UserRepository userRepository;

    @Override
    @UseCaseTransaction
    public ExamView createExam(CreateExamCommand command, UUID actorId) {
        if (command == null || command.subjectId() == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Subject ID is required");
        }
        accessGuard.requireManage(actorId, command.subjectId());
        Exam exam = Exam.create(command.subjectId(), actorId, command.title(), command.durationSeconds(),
                command.answerTimeLimitSeconds(), command.maxFollowUpsPerQuestion());
        Exam saved = examRepository.save(exam);
        return toView(saved, 0);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<ExamView> listExamsForSubject(UUID subjectId, UUID actorId) {
        if (subjectId == null) {
            return List.of();
        }
        accessGuard.requireManage(actorId, subjectId);
        boolean admin = isAdmin(actorId);
        List<Exam> exams = examRepository.findBySubjectId(subjectId);
        return exams.stream()
                .filter(e -> admin || e.getCreatorId().equals(actorId))
                .map(e -> {
                    int count = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(e.getId()).size();
                    return toView(e, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public ExamDetailView getExam(UUID examId, UUID actorId) {
        Exam exam = findExam(examId);
        checkExamAccess(exam, actorId);
        List<ExamQuestion> questions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId);
        List<ExamQuestionView> questionViews = questions.stream()
                .map(eq -> {
                    Optional<QuestionVersion> verOpt = questionVersionRepository.findById(eq.getQuestionVersionId());
                    String content = verOpt.map(QuestionVersion::getQuestionContent).orElse("");
                    String answer = verOpt.map(QuestionVersion::getReferenceAnswer).orElse("");
                    return new ExamQuestionView(
                            eq.getId(),
                            eq.getExamId(),
                            eq.getQuestionId(),
                            eq.getQuestionVersionId(),
                            eq.getOrderIndex(),
                            content,
                            answer
                    );
                })
                .collect(Collectors.toList());
        return new ExamDetailView(toView(exam, questions.size()), questionViews);
    }

    @Override
    @UseCaseTransaction
    public ExamView updateExamTitle(UUID examId, UpdateExamCommand command, UUID actorId) {
        Exam exam = findExamForUpdate(examId);
        checkExamAccess(exam, actorId);
        if (!exam.isDraft()) {
            throw new ApplicationException(ErrorCode.EXAM_NOT_DRAFT);
        }
        if (command == null || command.isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam title is required");
        }
        if (command.title() != null) {
            if (command.title().isBlank()) {
                throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam title is required");
            }
            exam.updateTitle(command.title());
        }
        exam.applyInterviewSettings(command.answerTimeLimitSeconds(), command.maxFollowUpsPerQuestion());
        Exam saved = examRepository.save(exam);
        int count = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId).size();
        return toView(saved, count);
    }

    @Override
    @UseCaseTransaction
    public ExamView updateExamStatus(UUID examId, UpdateExamStatusCommand command, UUID actorId) {
        Exam exam = findExamForUpdate(examId);
        checkExamAccess(exam, actorId);
        if (command == null || command.status() == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam status is required");
        }
        if (command.status() == ExamStatus.IN_PROGRESS) {
            int count = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId).size();
            if (count == 0) {
                throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Cannot publish exam with zero questions");
            }
        }
        exam.transitionTo(command.status());
        Exam saved = examRepository.save(exam);
        int count = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId).size();
        return toView(saved, count);
    }

    @Override
    @UseCaseTransaction
    public void deleteExam(UUID examId, UUID actorId) {
        Exam exam = findExamForUpdate(examId);
        checkExamAccess(exam, actorId);
        if (!exam.isDraft()) {
            throw new ApplicationException(ErrorCode.EXAM_NOT_DRAFT);
        }
        if (examAttemptRepository.countByExamId(examId) > 0) {
            throw new ApplicationException(ErrorCode.EXAM_NOT_DRAFT, "Cannot delete exam with existing attempts");
        }
        examQuestionRepository.deleteByExamId(examId);
        examRepository.deleteById(examId);
    }

    @Override
    @UseCaseTransaction
    public ExamDetailView setExamQuestions(UUID examId, SetExamQuestionsCommand command, UUID actorId) {
        Exam exam = findExamForUpdate(examId);
        checkExamAccess(exam, actorId);
        if (!exam.isDraft()) {
            throw new ApplicationException(ErrorCode.EXAM_NOT_DRAFT);
        }
        if (examAttemptRepository.countByExamId(examId) > 0) {
            throw new ApplicationException(ErrorCode.EXAM_NOT_DRAFT, "Cannot replace questions for exam with existing attempts");
        }
        if (command == null || command.questions() == null || command.questions().isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Questions list cannot be empty");
        }

        Set<UUID> questionIds = new HashSet<>();
        Set<Integer> orderIndices = new HashSet<>();
        List<ExamQuestion> newQuestions = new ArrayList<>();

        for (SetExamQuestionsCommand.ExamQuestionItem item : command.questions()) {
            if (item.questionId() == null || item.questionVersionId() == null) {
                throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Question ID and Version ID are required");
            }
            if (!questionIds.add(item.questionId())) {
                throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Duplicate question in exam: " + item.questionId());
            }
            if (!orderIndices.add(item.orderIndex())) {
                throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Duplicate order index in exam: " + item.orderIndex());
            }

            Question q = questionRepository.findById(item.questionId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_NOT_FOUND));
            if (!q.getSubjectId().equals(exam.getSubjectId())) {
                throw new ApplicationException(ErrorCode.EXAM_QUESTION_MISMATCH, "Question does not belong to exam subject");
            }

            QuestionVersion ver = questionVersionRepository.findById(item.questionVersionId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_VERSION_NOT_FOUND));
            if (!ver.getQuestionId().equals(item.questionId()) || ver.getApprovalStatus() != QuestionStatus.APPROVED) {
                throw new ApplicationException(ErrorCode.EXAM_QUESTION_MISMATCH, "Question version must be approved and match question");
            }

            newQuestions.add(ExamQuestion.create(examId, item.questionId(), item.questionVersionId(), item.orderIndex()));
        }

        examQuestionRepository.deleteByExamId(examId);
        examQuestionRepository.saveAll(newQuestions);

        return getExam(examId, actorId);
    }

    private Exam findExam(UUID examId) {
        if (examId == null) {
            throw new ApplicationException(ErrorCode.EXAM_NOT_FOUND);
        }
        return examRepository.findById(examId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXAM_NOT_FOUND));
    }

    private Exam findExamForUpdate(UUID examId) {
        if (examId == null) {
            throw new ApplicationException(ErrorCode.EXAM_NOT_FOUND);
        }
        return examRepository.findByIdForUpdate(examId)
                .or(() -> examRepository.findById(examId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.EXAM_NOT_FOUND));
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

    private void checkExamAccess(Exam exam, UUID actorId) {
        accessGuard.requireManage(actorId, exam.getSubjectId());
        if (exam.getCreatorId() != null && !isAdmin(actorId) && !exam.getCreatorId().equals(actorId)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Only the exam creator can access or modify this exam");
        }
    }

    private ExamView toView(Exam exam, int questionCount) {
        return new ExamView(
                exam.getId(),
                exam.getSubjectId(),
                exam.getCreatorId(),
                exam.getTitle(),
                exam.getDurationSeconds(),
                exam.getStatus(),
                questionCount,
                exam.getCreatedAt(),
                exam.getUpdatedAt(),
                exam.getAnswerTimeLimitSeconds(),
                exam.getMaxFollowUpsPerQuestion()
        );
    }
}
