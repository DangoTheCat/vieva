package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.common.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.StartExamUseCase;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.*;

import java.time.Instant;
import java.util.*;

public class StartExamUseCaseImpl implements StartExamUseCase {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;
    private final ExamAttemptQuestionSnapshotRepository snapshotRepository;

    private RubricRepository rubricRepository;
    private RubricCriterionRepository rubricCriterionRepository;
    private JsonSerializerPort jsonSerializerPort;

    public StartExamUseCaseImpl(
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            QuestionRepository questionRepository,
            QuestionVersionRepository questionVersionRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository
    ) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examTurnRepository = examTurnRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public StartExamUseCaseImpl(
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            QuestionRepository questionRepository,
            QuestionVersionRepository questionVersionRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            JsonSerializerPort jsonSerializerPort
    ) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examTurnRepository = examTurnRepository;
        this.snapshotRepository = snapshotRepository;
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.jsonSerializerPort = jsonSerializerPort;
    }

    public void setRubricServices(
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            JsonSerializerPort jsonSerializerPort
    ) {
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.jsonSerializerPort = jsonSerializerPort;
    }

    @Override
    @UseCaseTransaction
    public ExamAttemptView startExam(UUID studentId, UUID subjectId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND, "Student not found: " + studentId));
        if (student.getStatus() != UserStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.USER_INACTIVE, "Student account is not active");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_NOT_FOUND, "Subject not found: " + subjectId));
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.SUBJECT_INACTIVE, "Subject is not active");
        }

        // Paging beyond 50 questions to fetch all approved questions for subject
        List<Question> bankQuestions = new ArrayList<>();
        int page = 0;
        while (true) {
            var searchCriteria = QuestionBankSearchCriteria.builder()
                    .subjectId(subjectId)
                    .page(page)
                    .size(50)
                    .build();
            var pagedQuestions = questionRepository.searchBank(searchCriteria);
            if (pagedQuestions == null || pagedQuestions.getContent() == null || pagedQuestions.getContent().isEmpty()) {
                break;
            }
            bankQuestions.addAll(pagedQuestions.getContent());
            page++;
            if (page >= pagedQuestions.getTotalPages()) {
                break;
            }
        }

        if (bankQuestions.isEmpty()) {
            throw new ApplicationException(ErrorCode.QUESTION_NOT_FOUND, "No approved questions found in question bank for subject: " + subjectId);
        }

        // Stable ordering by questionId
        bankQuestions.sort(Comparator.comparing(Question::getQuestionId));

        // Select first approved question for the attempt
        Question firstQuestion = bankQuestions.get(0);
        UUID approvedVersionId = firstQuestion.getCurrentApprovedVersionId();
        if (approvedVersionId == null) {
            throw new ApplicationException(ErrorCode.NO_APPROVED_VERSION, "Question has no approved version: " + firstQuestion.getQuestionId());
        }

        QuestionVersion questionVersion = questionVersionRepository.findById(approvedVersionId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_VERSION_NOT_FOUND, "Approved question version not found: " + approvedVersionId));

        int totalQuestions = bankQuestions.size();

        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, totalQuestions);
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        // Freeze bank selection at initialization into snapshots
        if (snapshotRepository != null) {
            List<ExamAttemptQuestionSnapshot> snapshots = new ArrayList<>();
            int order = 0;
            for (Question q : bankQuestions) {
                UUID vId = q.getCurrentApprovedVersionId();
                if (vId != null) {
                    Optional<QuestionVersion> verOpt = questionVersionRepository.findById(vId);
                    if (verOpt.isPresent()) {
                        QuestionVersion v = verOpt.get();
                        String rubricJson = "[]";
                        if (rubricRepository != null && rubricCriterionRepository != null && jsonSerializerPort != null) {
                            Optional<Rubric> rubricOpt = rubricRepository.findByQuestionVersionId(vId);
                            if (rubricOpt.isPresent()) {
                                List<RubricCriterion> criteria = rubricCriterionRepository.findByRubricId(rubricOpt.get().getRubricId());
                                rubricJson = jsonSerializerPort.serialize(criteria);
                            }
                        }

                        snapshots.add(ExamAttemptQuestionSnapshot.create(
                                savedAttempt.getId(),
                                q.getQuestionId(),
                                vId,
                                order++,
                                v.getQuestionContent(),
                                v.getReferenceAnswer(),
                                rubricJson
                        ));
                    }
                }
            }
            if (!snapshots.isEmpty()) {
                snapshotRepository.saveAll(snapshots);
            }
        }

        ExamTurn firstTurn = ExamTurn.createMainQuestion(
                savedAttempt.getId(),
                firstQuestion.getQuestionId(),
                questionVersion.getQuestionVersionId(),
                1,
                questionVersion.getQuestionContent(),
                null // TTS audio generated asynchronously or on-demand
        );
        ExamTurn savedTurn = examTurnRepository.save(firstTurn);

        return new ExamAttemptView(
                savedAttempt.getId(),
                savedAttempt.getStudentId(),
                savedAttempt.getSubjectId(),
                savedAttempt.getStatus().name(),
                savedAttempt.getCurrentQuestionIndex(),
                savedAttempt.getTotalQuestions(),
                savedAttempt.getTotalScore(),
                savedAttempt.getGradeLevel() != null ? savedAttempt.getGradeLevel().name() : null,
                savedAttempt.getStartedAt(),
                savedAttempt.getCompletedAt(),
                savedAttempt.getPausedAt(),
                savedAttempt.remainingSecondsAt(Instant.now()),
                savedAttempt.getGraceDeadline(),
                savedTurn.getId(),
                savedTurn.getQuestionText(),
                savedTurn.getTtsAudioUrl()
        );
    }
}
