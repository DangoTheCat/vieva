package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.common.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.input.exam.SubmitTurnAnswerUseCase;
import com.example.vieva.application.ports.input.exam.TurnEvaluationView;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.domain.entities.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
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
    private final TransactionRunnerPort transactionRunner;

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
            EmbeddingModelPort embeddingModel,
            TransactionRunnerPort transactionRunner
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
        this.transactionRunner = transactionRunner;
    }

    /**
     * Records the answer, evaluates it with the LLM and moves the interview on. The slow work
     * (embedding + LLM) runs between two short transactions so the attempt row is never locked
     * while waiting for the model (plan item 2.7).
     */
    @Override
    public TurnEvaluationView submitAnswer(
            UUID attemptId,
            UUID turnId,
            UUID studentId,
            String transcriptText,
            byte[] audioBytes,
            String audioContentType
    ) {
        // Step 1 — short transaction, attempt row locked: validate and store the answer
        RecordedAnswer recorded = transactionRunner.inNewTransaction(
                () -> recordAnswer(attemptId, turnId, studentId, transcriptText, audioBytes, audioContentType));
        if (recorded.finishedView() != null) {
            return recorded.finishedView();
        }

        // Step 2 — no transaction: retrieval and LLM evaluation
        String ragContext = buildRagContext(recorded.subjectId(), recorded.turn().getQuestionText(), transcriptText);
        VivaEvaluationContext evalContext = new VivaEvaluationContext(
                recorded.expectedAnswer(), recorded.rubricJson(), ragContext);
        VivaEvaluationResult evalResult = vivaEvaluationGateway.evaluateTurn(
                AiGradingInputs.toEvaluationTurn(recorded.turn()), AiGradingInputs.toRubricSummary(recorded.rubric()), evalContext);

        // Step 3 — short transaction, attempt row locked again: apply the result, pick the next turn
        return transactionRunner.inNewTransaction(() -> applyEvaluation(attemptId, turnId, evalResult));
    }

    /** Outcome of step 1: either a final view (nothing to evaluate) or what the evaluator needs. */
    private record RecordedAnswer(TurnEvaluationView finishedView, ExamTurn turn, UUID subjectId,
                                  String expectedAnswer, String rubricJson, Rubric rubric) {
        static RecordedAnswer finished(TurnEvaluationView view) {
            return new RecordedAnswer(view, null, null, null, null, null);
        }
    }

    private RecordedAnswer recordAnswer(UUID attemptId, UUID turnId, UUID studentId, String transcriptText,
                                        byte[] audioBytes, String audioContentType) {
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
            return RecordedAnswer.finished(new TurnEvaluationView(
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
            ));
        }

        // Reject new unevaluated answers when attempt status is not in progress
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST,
                    "Exam attempt is not IN_PROGRESS (current: " + attempt.getStatus() + ")");
        }

        // Server-side exam clock: an answer arriving well after time ran out is not graded;
        // the attempt is closed and finalized instead. Within the grace window it is graded,
        // then the attempt ends without asking another question.
        Instant now = Instant.now();
        if (attempt.isPastSubmitGrace(now)) {
            log.info("Attempt {} ran out of time before turn {} was submitted; finalizing", attemptId, turnId);
            attempt.timeoutSession();
            examAttemptRepository.save(attempt);
            dispatchFinalization(attemptId, attempt);
            return RecordedAnswer.finished(new TurnEvaluationView(
                    currentTurn.getId(),
                    currentTurn.getAttemptId(),
                    currentTurn.getTurnType() != null ? currentTurn.getTurnType().name() : TurnType.MAIN_QUESTION.name(),
                    currentTurn.getTurnOrder(),
                    currentTurn.getStatus().name(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null,
                    null,
                    now,
                    0,
                    true
            ));
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
            String contentType = audioContentType != null && !audioContentType.isBlank() ? audioContentType : "audio/webm";
            String storagePath = String.format("oral-exams/%s/%s.%s", attemptId, turnId, audioExtension(contentType));
            StoredFile storedFile = fileStoragePort.store(storagePath, audioBytes, contentType);
            studentAudioUrl = storedFile != null ? storedFile.url() : null;
        }

        // Update current turn answer; late answers (after the per-answer deadline + grace) are flagged
        currentTurn.submitAnswer(transcriptText, studentAudioUrl, now,
                attempt.effectiveAnswerTimeLimitSeconds(), ExamAttempt.SUBMIT_GRACE_SECONDS);

        examTurnRepository.save(currentTurn);

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

        if (rubricRepository != null && currentTurn.getQuestionVersionId() != null && rubricJson == null) {
            rubric = rubricRepository.findByQuestionVersionId(currentTurn.getQuestionVersionId()).orElse(null);
        }

        return new RecordedAnswer(null, currentTurn, attempt.getSubjectId(), expectedAnswer, rubricJson, rubric);
    }

    /** Retrieves course-material context for the evaluator. Runs outside any transaction (embedding call). */
    private String buildRagContext(UUID subjectId, String questionTextOfTurn, String transcriptText) {
        // Build scoped RAG context if subject documents and embedding model are available
        String ragContext = null;
        if (courseDocumentRepository != null && documentChunkRepository != null && embeddingModel != null && subjectId != null) {
            List<CourseDocument> activeDocs = courseDocumentRepository.findActiveBySubjectId(subjectId);
            List<CourseDocument> readyDocs = activeDocs != null
                    ? activeDocs.stream().filter(CourseDocument::isReady).toList()
                    : List.of();
            if (!readyDocs.isEmpty()) {
                List<UUID> readyDocIds = readyDocs.stream().map(CourseDocument::getDocumentId).toList();
                String questionText = questionTextOfTurn != null ? questionTextOfTurn : "";
                String transcript = transcriptText != null ? transcriptText : "";
                String queryString = (questionText + " " + transcript).trim();
                if (!queryString.isBlank()) {
                    float[] queryEmbedding = embeddingModel.generateEmbedding(queryString);
                    if (queryEmbedding != null && queryEmbedding.length > 0) {
                        List<ChunkSearchResult> chunks = documentChunkRepository.searchSimilarChunks(
                                subjectId,
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

        return ragContext;
    }

    private TurnEvaluationView applyEvaluation(UUID attemptId, UUID turnId, VivaEvaluationResult evalResult) {
        ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(attemptId)
                .or(() -> examAttemptRepository.findById(attemptId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam attempt not found: " + attemptId));
        ExamTurn currentTurn = examTurnRepository.findById(turnId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Turn not found: " + turnId));
        if (currentTurn.getStatus() != TurnStatus.ANSWERED) {
            // A concurrent request already evaluated this turn while the LLM was running
            throw new ApplicationException(ErrorCode.INVALID_REQUEST,
                    "Turn is no longer awaiting evaluation (current: " + currentTurn.getStatus() + ")");
        }
        // The clock may have run out (or the student finished) while the answer was being evaluated:
        // grade this answer, then end the attempt instead of asking another question.
        boolean attemptStillRunning = attempt.getStatus() == AttemptStatus.IN_PROGRESS;
        boolean sessionTimeUp = !attemptStillRunning || attempt.isTimeExpired(Instant.now());

        currentTurn.evaluate(
                evalResult.score(),
                evalResult.feedback(),
                evalResult.rubricEvaluations()
        );
        examTurnRepository.save(currentTurn);

        // Enforce the follow-up budget frozen from the exam (default 2 per main question)
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

        boolean canFollowUp = !sessionTimeUp
                && evalResult.shouldFollowUp()
                && !evalResult.isStandardMet()
                && followUpCount < attempt.effectiveMaxFollowUps()
                && evalResult.followUpQuestionText() != null
                && !evalResult.followUpQuestionText().isBlank();

        UUID nextTurnId = null;
        String nextQuestionText = null;
        String nextTurnType = null;
        boolean isAttemptFinished = false;

        List<ExamTurn> allAttemptTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
        int nextTurnOrder = allAttemptTurns.size() + 1;

        if (sessionTimeUp) {
            // Time ran out while this answer was in flight: grade it, then close the attempt
            isAttemptFinished = true;
            if (attemptStillRunning) {
                attempt.timeoutSession();
                examAttemptRepository.save(attempt);
                dispatchFinalization(attemptId, attempt);
            }
        } else if (canFollowUp) {
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
                currentTurn.getUpdatedAt(),
                attempt.remainingAt(Instant.now()),
                currentTurn.isTimedOut()
        );
    }

    private static String audioExtension(String contentType) {
        String type = contentType.toLowerCase();
        if (type.contains("webm")) return "webm";
        if (type.contains("ogg")) return "ogg";
        if (type.contains("mp4") || type.contains("m4a") || type.contains("aac")) return "m4a";
        if (type.contains("mpeg") || type.contains("mp3")) return "mp3";
        return "wav";
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
