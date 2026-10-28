package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.common.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.input.exam.SubmitTurnAnswerUseCase;
import com.example.vieva.application.ports.input.exam.TurnEvaluationView;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class SubmitTurnAnswerUseCaseImpl implements SubmitTurnAnswerUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final FileStoragePort fileStoragePort;
    private final VivaEvaluationGateway vivaEvaluationGateway;
    private final AttemptGradingService attemptGradingService;
    private final ExamAttemptQuestionSnapshotRepository snapshotRepository;
    private final ExamFinalizationPort examFinalizationPort;
    private final CourseDocumentRepository courseDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingModelPort embeddingModel;

    public SubmitTurnAnswerUseCaseImpl(
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            QuestionRepository questionRepository,
            QuestionVersionRepository questionVersionRepository,
            RubricRepository rubricRepository,
            FileStoragePort fileStoragePort,
            VivaEvaluationGateway vivaEvaluationGateway,
            AttemptGradingService attemptGradingService,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            ExamFinalizationPort examFinalizationPort,
            CourseDocumentRepository courseDocumentRepository,
            DocumentChunkRepository documentChunkRepository,
            EmbeddingModelPort embeddingModel
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.examTurnRepository = examTurnRepository;
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.rubricRepository = rubricRepository;
        this.fileStoragePort = fileStoragePort;
        this.vivaEvaluationGateway = vivaEvaluationGateway;
        this.attemptGradingService = attemptGradingService;
        this.snapshotRepository = snapshotRepository;
        this.examFinalizationPort = examFinalizationPort;
        this.courseDocumentRepository = courseDocumentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingModel = embeddingModel;
    }

    public SubmitTurnAnswerUseCaseImpl(
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            QuestionRepository questionRepository,
            QuestionVersionRepository questionVersionRepository,
            RubricRepository rubricRepository,
            FileStoragePort fileStoragePort,
            VivaEvaluationGateway vivaEvaluationGateway,
            AttemptGradingService attemptGradingService,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            ExamFinalizationPort examFinalizationPort
    ) {
        this(
                examAttemptRepository,
                examTurnRepository,
                questionRepository,
                questionVersionRepository,
                rubricRepository,
                fileStoragePort,
                vivaEvaluationGateway,
                attemptGradingService,
                snapshotRepository,
                examFinalizationPort,
                null,
                null,
                null
        );
    }

    public SubmitTurnAnswerUseCaseImpl(
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            QuestionRepository questionRepository,
            QuestionVersionRepository questionVersionRepository,
            RubricRepository rubricRepository,
            FileStoragePort fileStoragePort,
            VivaEvaluationGateway vivaEvaluationGateway,
            AttemptGradingService attemptGradingService
    ) {
        this(
                examAttemptRepository,
                examTurnRepository,
                questionRepository,
                questionVersionRepository,
                rubricRepository,
                fileStoragePort,
                vivaEvaluationGateway,
                attemptGradingService,
                null,
                attemptId -> {
                    if (attemptGradingService != null) {
                        examAttemptRepository.findById(attemptId).ifPresent(attempt -> {
                            List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
                            attemptGradingService.gradeAndCompleteAttempt(attempt, turns);
                        });
                    }
                }
        );
    }

    @Override
    @UseCaseTransaction
    public TurnEvaluationView submitAnswer(
            UUID attemptId,
            UUID turnId,
            UUID studentId,
            String transcriptText,
            byte[] audioBytes,
            String audioContentType
    ) {
        // Lock attempt to serialize concurrent submissions
        ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(attemptId)
                .or(() -> examAttemptRepository.findById(attemptId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Student does not own this exam attempt");
        }

        ExamTurn currentTurn = examTurnRepository.findById(turnId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Turn not found: " + turnId));

        if (!currentTurn.getAttemptId().equals(attemptId)) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Turn does not belong to this attempt");
        }

        // Idempotency: return recorded result for already evaluated turn even after finalization
        if (currentTurn.getStatus() == TurnStatus.EVALUATED) {
            List<ExamTurn> allAttemptTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
            ExamTurn nextTurn = allAttemptTurns.stream()
                    .filter(t -> t.getTurnOrder() > currentTurn.getTurnOrder())
                    .findFirst()
                    .orElse(null);
            boolean isAttemptFinished = attempt.getStatus() == AttemptStatus.FINALIZING
                    || attempt.getStatus() == AttemptStatus.COMPLETED;
            return new TurnEvaluationView(
                    currentTurn.getId(),
                    currentTurn.getAttemptId(),
                    currentTurn.getTurnType() != null ? currentTurn.getTurnType().name() : "MAIN_QUESTION",
                    currentTurn.getTurnOrder(),
                    currentTurn.getStatus().name(),
                    currentTurn.getScore(),
                    currentTurn.getFeedback(),
                    currentTurn.getRubricEvaluations(),
                    currentTurn.getStudentAudioUrl(),
                    currentTurn.getTranscriptText(),
                    isAttemptFinished,
                    nextTurn != null ? nextTurn.getId() : null,
                    nextTurn != null ? nextTurn.getQuestionText() : null,
                    nextTurn != null && nextTurn.getTurnType() != null ? nextTurn.getTurnType().name() : null,
                    currentTurn.getUpdatedAt()
            );
        }

        // Reject new unevaluated answers when attempt status is not in progress
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST,
                    "Exam attempt is not IN_PROGRESS (current: " + attempt.getStatus() + ")");
        }

        // Time is up on the server clock; ExamAttemptTimeoutScheduler finalizes the attempt
        if (attempt.isTimeUp(java.time.Instant.now())) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam time is up");
        }

        // Reject empty or blank transcript AND empty audio
        boolean hasTranscript = transcriptText != null && !transcriptText.trim().isEmpty();
        boolean hasAudio = audioBytes != null && audioBytes.length > 0;
        if (!hasTranscript && !hasAudio) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Either transcript text or student audio must be provided");
        }

        // Upload audio if provided
        String studentAudioUrl = null;
        if (hasAudio) {
            String storagePath = String.format("oral-exams/%s/%s.wav", attemptId, turnId);
            String contentType = audioContentType != null && !audioContentType.isBlank() ? audioContentType : "audio/wav";
            StoredFile storedFile = fileStoragePort.store(storagePath, audioBytes, contentType);
            studentAudioUrl = storedFile != null ? storedFile.url() : null;
        }

        // Update current turn answer
        currentTurn.submitAnswer(transcriptText, studentAudioUrl);

        // Build scoped RAG context if subject documents and embedding model are available
        String ragContext = null;
        if (courseDocumentRepository != null && documentChunkRepository != null && embeddingModel != null && attempt.getSubjectId() != null) {
            List<CourseDocument> activeDocs = courseDocumentRepository.findActiveBySubjectId(attempt.getSubjectId());
            List<CourseDocument> readyDocs = activeDocs != null
                    ? activeDocs.stream().filter(CourseDocument::isReady).toList()
                    : List.of();
            if (!readyDocs.isEmpty()) {
                List<UUID> readyDocIds = readyDocs.stream().map(CourseDocument::getDocumentId).toList();
                String questionText = currentTurn.getQuestionText() != null ? currentTurn.getQuestionText() : "";
                String transcript = transcriptText != null ? transcriptText : "";
                String queryString = (questionText + " " + transcript).trim();
                if (!queryString.isBlank()) {
                    float[] queryEmbedding = embeddingModel.generateEmbedding(queryString);
                    if (queryEmbedding != null && queryEmbedding.length > 0) {
                        List<ChunkSearchResult> chunks = documentChunkRepository.searchSimilarChunks(
                                attempt.getSubjectId(),
                                readyDocIds,
                                queryEmbedding,
                                0.2,
                                3
                        );
                        if (chunks != null && !chunks.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (ChunkSearchResult c : chunks) {
                                if (sb.length() > 0) {
                                    sb.append("\n\n");
                                }
                                String docName = c.documentName() != null ? c.documentName() : "Tài liệu";
                                sb.append("[Tài liệu: ").append(docName).append("]\n").append(c.content());
                            }
                            ragContext = sb.toString();
                        }
                    }
                }
            }
        }

        // Fetch frozen snapshot for current question to supply expected answer and rubric criteria
        String expectedAnswer = null;
        String rubricJson = null;
        Rubric rubric = null;

        if (snapshotRepository != null && currentTurn.getQuestionId() != null) {
            Optional<ExamAttemptQuestionSnapshot> snapshotOpt = snapshotRepository.findByAttemptIdAndQuestionId(
                    attemptId, currentTurn.getQuestionId()
            );
            if (snapshotOpt.isEmpty()) {
                List<ExamAttemptQuestionSnapshot> allSnaps = snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId);
                snapshotOpt = allSnaps.stream()
                        .filter(s -> currentTurn.getQuestionId().equals(s.getQuestionId()))
                        .findFirst();
            }
            if (snapshotOpt.isPresent()) {
                ExamAttemptQuestionSnapshot snap = snapshotOpt.get();
                expectedAnswer = snap.getExpectedAnswer();
                rubricJson = snap.getRubricSnapshot();
            }
        }

        VivaEvaluationContext evalContext = new VivaEvaluationContext(expectedAnswer, rubricJson, ragContext);

        if (rubricRepository != null && currentTurn.getQuestionVersionId() != null && rubricJson == null) {
            rubric = rubricRepository.findByQuestionVersionId(currentTurn.getQuestionVersionId()).orElse(null);
        }

        // Evaluate turn with frozen expected answer and rubric
        VivaEvaluationResult evalResult = vivaEvaluationGateway.evaluateTurn(currentTurn, rubric, evalContext);
        currentTurn.evaluate(
                evalResult.score(),
                evalResult.feedback(),
                evalResult.rubricEvaluations()
        );
        examTurnRepository.save(currentTurn);

        // Enforce max 2 followups per main question
        List<ExamTurn> questionTurns = examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(
                attemptId, currentTurn.getQuestionId()
        );
        if (questionTurns.isEmpty()) {
            questionTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId).stream()
                    .filter(t -> currentTurn.getQuestionId().equals(t.getQuestionId()))
                    .toList();
        }

        long followUpCount = questionTurns.stream()
                .filter(t -> t.getTurnType() == TurnType.FOLLOW_UP)
                .count();

        boolean canFollowUp = evalResult.shouldFollowUp()
                && !evalResult.isStandardMet()
                && followUpCount < 2
                && evalResult.followUpQuestionText() != null
                && !evalResult.followUpQuestionText().isBlank();

        UUID nextTurnId = null;
        String nextQuestionText = null;
        String nextTurnType = null;
        boolean isAttemptFinished = false;

        List<ExamTurn> allAttemptTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
        int nextTurnOrder = allAttemptTurns.size() + 1;

        if (canFollowUp) {
            ExamTurn followUpTurn = ExamTurn.createFollowUp(
                    attemptId,
                    currentTurn.getQuestionId(),
                    currentTurn.getQuestionVersionId(),
                    currentTurn.getId(),
                    nextTurnOrder,
                    evalResult.followUpQuestionText(),
                    null
            );
            ExamTurn savedFollowUp = examTurnRepository.save(followUpTurn);
            nextTurnId = savedFollowUp.getId();
            nextQuestionText = savedFollowUp.getQuestionText();
            nextTurnType = TurnType.FOLLOW_UP.name();
        } else {
            // Check frozen snapshots first
            List<ExamAttemptQuestionSnapshot> snapshots = snapshotRepository != null
                    ? snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId)
                    : List.of();

            if (!snapshots.isEmpty()) {
                // MUST use question snapshots for next questions! NEVER call live questionRepository.searchBank(...)
                int nextIndex = attempt.getCurrentQuestionIndex() + 1;
                if (nextIndex < snapshots.size()) {
                    attempt.nextQuestion();
                    examAttemptRepository.save(attempt);

                    ExamAttemptQuestionSnapshot nextSnapshot = snapshots.get(nextIndex);
                    ExamTurn nextMainTurn = ExamTurn.createMainQuestion(
                            attemptId,
                            nextSnapshot.getQuestionId(),
                            nextSnapshot.getQuestionVersionId(),
                            nextTurnOrder,
                            nextSnapshot.getQuestionText(),
                            null
                    );
                    ExamTurn savedNextMainTurn = examTurnRepository.save(nextMainTurn);
                    nextTurnId = savedNextMainTurn.getId();
                    nextQuestionText = savedNextMainTurn.getQuestionText();
                    nextTurnType = TurnType.MAIN_QUESTION.name();
                } else {
                    // All frozen questions finished, trigger finalization
                    isAttemptFinished = true;
                    attempt.finish();
                    examAttemptRepository.save(attempt);
                    dispatchFinalization(attemptId, attempt);
                }
            } else {
                // Honest compatible legacy fallback for un-snapshotted attempts
                var searchCriteria = QuestionBankSearchCriteria.builder()
                        .subjectId(attempt.getSubjectId())
                        .page(0)
                        .size(50)
                        .build();
                var pagedQuestions = questionRepository != null ? questionRepository.searchBank(searchCriteria) : null;
                List<Question> bankQuestions = pagedQuestions != null && pagedQuestions.getContent() != null
                        ? pagedQuestions.getContent()
                        : Collections.emptyList();

                int nextIndex = attempt.getCurrentQuestionIndex() + 1;
                if (nextIndex < bankQuestions.size()) {
                    attempt.nextQuestion();
                    examAttemptRepository.save(attempt);

                    Question nextQuestion = bankQuestions.get(nextIndex);
                    UUID nextApprovedVersionId = nextQuestion.getCurrentApprovedVersionId();
                    QuestionVersion nextVersion = nextApprovedVersionId != null && questionVersionRepository != null
                            ? questionVersionRepository.findById(nextApprovedVersionId).orElse(null)
                            : null;

                    String qContent = nextVersion != null ? nextVersion.getQuestionContent() : "";
                    UUID vId = nextVersion != null ? nextVersion.getQuestionVersionId() : null;

                    ExamTurn nextMainTurn = ExamTurn.createMainQuestion(
                            attemptId,
                            nextQuestion.getQuestionId(),
                            vId,
                            nextTurnOrder,
                            qContent,
                            null
                    );
                    ExamTurn savedNextMainTurn = examTurnRepository.save(nextMainTurn);
                    nextTurnId = savedNextMainTurn.getId();
                    nextQuestionText = savedNextMainTurn.getQuestionText();
                    nextTurnType = TurnType.MAIN_QUESTION.name();
                } else {
                    // All questions finished, complete attempt
                    isAttemptFinished = true;
                    attempt.finish();
                    examAttemptRepository.save(attempt);
                    dispatchFinalization(attemptId, attempt);
                }
            }
        }

        return new TurnEvaluationView(
                currentTurn.getId(),
                currentTurn.getAttemptId(),
                currentTurn.getTurnType().name(),
                currentTurn.getTurnOrder(),
                currentTurn.getStatus().name(),
                currentTurn.getScore(),
                currentTurn.getFeedback(),
                currentTurn.getRubricEvaluations(),
                currentTurn.getStudentAudioUrl(),
                currentTurn.getTranscriptText(),
                isAttemptFinished,
                nextTurnId,
                nextQuestionText,
                nextTurnType,
                currentTurn.getUpdatedAt()
        );
    }

    private void dispatchFinalization(UUID attemptId, ExamAttempt attempt) {
        if (examFinalizationPort != null) {
            examFinalizationPort.dispatchFinalization(attemptId);
        } else if (attemptGradingService != null) {
            List<ExamTurn> finalizedTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
            attemptGradingService.gradeAndCompleteAttempt(attempt, finalizedTurns);
        }
    }
}
