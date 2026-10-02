package com.example.vieva.application.usecases.generation;

import com.example.vieva.application.ports.input.GenerateQuestionsCommand;
import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.GenerationResult;
import com.example.vieva.application.ports.output.QuestionGenerationPort;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.example.vieva.application.ports.output.QuestionGenerationRequestRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.application.settings.RagSettings;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.generation.GeneratedQuestionValidator.Outcome;
import com.example.vieva.application.usecases.generation.GeneratedQuestionValidator.ValidatedQuestion;
import com.example.vieva.application.usecases.generation.GeneratedQuestionValidator.ValidatedSource;
import com.example.vieva.application.usecases.question.QuestionAccessLoader;
import com.example.vieva.application.usecases.question.QuestionAccessLoader.VersionContext;
import com.example.vieva.application.usecases.question.QuestionDraftWriter;
import com.example.vieva.application.usecases.question.QuestionDraftWriter.NewDraft;
import com.example.vieva.application.usecases.question.QuestionVersionViewAssembler;
import com.example.vieva.domain.entities.BloomDistribution;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.Topic;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.services.QuestionSimilarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * LLM and embedding calls never run inside a database transaction: the request is persisted first
 * (PENDING), the AI work happens outside, and the valid drafts + outcome are written in one short
 * transaction that re-checks the lecturer's permission (BR-06).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationServiceImpl implements QuestionGenerationService {

    private static final int MAX_ISSUES = 50;

    private final QuestionGenerationRequestRepository generationRequestRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuestionSourceRepository questionSourceRepository;
    private final CourseDocumentRepository courseDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final EmbeddingModelPort embeddingModel;
    private final QuestionGenerationPort questionGenerator;
    private final TransactionRunnerPort transactionRunner;
    private final SubjectAccessGuard subjectAccessGuard;
    private final QuestionAccessLoader accessLoader;
    private final QuestionDraftWriter draftWriter;
    private final QuestionVersionViewAssembler viewAssembler;
    private final QuestionBankAuditor auditor;
    private final RagSettings settings;

    /** Retrieved context of a run, keyed by prompt ref (C1..Cn) and by chunk id. */
    private record Context(Subject subject, Topic topic, List<ChunkSearchResult> chunks,
                           Map<String, ChunkSearchResult> byRef) {
    }

    private record RunOutcome(List<ValidatedQuestion> accepted, int rejected, List<String> issues, String lastError) {
    }

    // ─── UC1.2: generate ──────────────────────────────────────────────────────

    @Override
    public GenerationResult generate(GenerateQuestionsCommand command, UUID actorId) {
        BloomDistribution distribution = validateCommand(command);
        QuestionGenerationRequest request = transactionRunner.inNewTransaction(() -> {
            subjectAccessGuard.requireManage(actorId, command.subjectId());
            requireSubject(command.subjectId());
            loadTopic(command.subjectId(), command.topicId());
            List<UUID> documentIds = requireReadyDocuments(command.subjectId(), command.documentIds());
            QuestionGenerationRequest created = QuestionGenerationRequest.start(command.subjectId(), command.topicId(),
                    actorId, documentIds, distribution, trimToNull(command.lecturerNote()));
            return generationRequestRepository.save(created);
        });

        Context context = retrieveContext(request);
        return runAndPersist(request, context, distribution, actorId);
    }

    @Override
    public GenerationResult retry(UUID generationRequestId, UUID actorId) {
        QuestionGenerationRequest request = transactionRunner.inNewTransaction(() -> {
            QuestionGenerationRequest loaded = generationRequestRepository.findById(generationRequestId)
                    .orElseThrow(() -> new AppException(ErrorCode.GENERATION_REQUEST_NOT_FOUND));
            subjectAccessGuard.requireManage(actorId, loaded.getSubjectId());
            loaded.ensureRetryable();
            if (loaded.getAttemptCount() >= settings.getMaxTotalAttempts()) {
                throw new AppException(ErrorCode.RETRY_LIMIT_EXCEEDED,
                        "Generation attempt limit reached (" + settings.getMaxTotalAttempts() + ")");
            }
            requireReadyDocuments(loaded.getSubjectId(), loaded.getDocumentIds());
            return loaded;
        });

        Map<BloomLevel, Integer> produced = new EnumMap<>(BloomLevel.class);
        questionVersionRepository.findByGenerationRequestId(generationRequestId)
                .forEach(v -> produced.merge(v.getBloomLevel(), 1, Integer::sum));
        BloomDistribution remaining = request.distribution().remainingAfter(produced);
        if (remaining == null) {
            QuestionGenerationRequest completed = transactionRunner.inNewTransaction(() -> {
                request.recordOutcome(0, 0, List.of(), null);
                return generationRequestRepository.save(request);
            });
            return new GenerationResult(completed, List.of());
        }

        Context context = request.getRetrievedChunkIds().isEmpty()
                ? retrieveContext(request)
                : storedContext(request);
        return runAndPersist(request, context, remaining, actorId);
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationResult getRequest(UUID generationRequestId) {
        QuestionGenerationRequest request = generationRequestRepository.findById(generationRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.GENERATION_REQUEST_NOT_FOUND));
        return new GenerationResult(request,
                viewAssembler.assemble(questionVersionRepository.findByGenerationRequestId(generationRequestId)));
    }

    // ─── UC1.2: regenerate one draft ─────────────────────────────────────────

    @Override
    public QuestionVersionView regenerate(UUID versionId, String feedback, Long expectedVersion, UUID actorId) {
        record Prepared(QuestionVersion version, QuestionGenerationRequest request, Context context,
                        List<Set<String>> otherQuestions) {
        }
        Prepared prepared = transactionRunner.inNewTransaction(() -> {
            VersionContext ctx = accessLoader.versionForWrite(versionId, actorId);
            QuestionVersion version = ctx.version();
            version.ensureVersionMatches(expectedVersion);
            version.ensureDraft();
            ctx.question().ensureActive();
            if (!version.isAiGenerated() || version.getGenerationRequestId() == null) {
                throw new AppException(ErrorCode.REGENERATION_NOT_SUPPORTED);
            }
            if (version.getRegenerationCount() >= settings.getMaxRegenerationsPerVersion()) {
                throw new AppException(ErrorCode.RETRY_LIMIT_EXCEEDED,
                        "Regeneration limit reached (" + settings.getMaxRegenerationsPerVersion() + ") for this draft");
            }
            QuestionGenerationRequest request = generationRequestRepository.findById(version.getGenerationRequestId())
                    .orElseThrow(() -> new AppException(ErrorCode.GENERATION_REQUEST_NOT_FOUND));
            Context context = storedContext(request);
            List<Set<String>> others = questionVersionRepository.findActiveContentsBySubject(request.getSubjectId()).stream()
                    .filter(content -> !Objects.equals(content, version.getQuestionContent()))
                    .map(QuestionSimilarity::tokens)
                    .collect(Collectors.toCollection(ArrayList::new));
            return new Prepared(version, request, context, others);
        });

        QuestionVersion original = prepared.version();
        BloomDistribution single = BloomDistribution.single(original.getBloomLevel());
        QuestionGenerationPrompt basePrompt = prompt(prepared.context(), single, prepared.request().getLecturerNote(),
                List.of(), original.getQuestionContent(), feedback);
        RunOutcome outcome = runAttempts(null, basePrompt, single, prepared.context(), prepared.otherQuestions());
        if (outcome.accepted().isEmpty()) {
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "No valid replacement was produced; the draft was kept unchanged"
                            + (outcome.lastError() != null ? " (" + outcome.lastError() + ")" : ""));
        }
        ValidatedQuestion replacement = outcome.accepted().get(0);

        return transactionRunner.inNewTransaction(() -> {
            // Re-check permission and that nobody edited the draft during the LLM call.
            VersionContext ctx = accessLoader.versionForWrite(versionId, actorId);
            QuestionVersion version = ctx.version();
            version.ensureVersionMatches(original.getVersion());
            Map<String, Object> before = new HashMap<>();
            before.put("content", version.getQuestionContent());
            before.put("bloomLevel", version.getBloomLevel());
            before.put("feedback", trimToNull(feedback));

            version.replaceWithRegenerated(replacement.content(), replacement.expectedAnswer(), replacement.bloomLevel());
            replaceRubricAndSources(version, replacement);
            questionVersionRepository.save(version);
            auditor.record(actorId, "QUESTION_REGENERATED", QuestionBankAuditor.QUESTION_VERSION, versionId, before,
                    Map.of("content", replacement.content(), "regenerationCount", version.getRegenerationCount()));
            return viewAssembler.assemble(questionVersionRepository.findById(versionId).orElseThrow());
        });
    }

    // ─── pipeline steps ──────────────────────────────────────────────────────

    private BloomDistribution validateCommand(GenerateQuestionsCommand command) {
        if (command.subjectId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "subjectId is required");
        }
        if (command.totalQuestions() < 1 || command.totalQuestions() > settings.getMaxQuestionsPerRequest()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "totalQuestions must be between 1 and " + settings.getMaxQuestionsPerRequest());
        }
        if (command.documentIds() == null || command.documentIds().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Select at least one READY document");
        }
        return BloomDistribution.of(command.bloomDistribution(), command.totalQuestions());
    }

    /** BR-07: every selected document exists, belongs to the subject and is READY. */
    private List<UUID> requireReadyDocuments(UUID subjectId, List<UUID> requestedIds) {
        List<UUID> ids = new ArrayList<>(new LinkedHashSet<>(requestedIds));
        Map<UUID, CourseDocument> documents = courseDocumentRepository.findAllByIds(ids).stream()
                .collect(Collectors.toMap(CourseDocument::getDocumentId, Function.identity()));
        for (UUID id : ids) {
            CourseDocument document = documents.get(id);
            if (document == null) {
                throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Document " + id + " not found");
            }
            if (!Objects.equals(document.getSubjectId(), subjectId)) {
                throw new AppException(ErrorCode.DOCUMENT_SUBJECT_MISMATCH,
                        "Document '" + document.getFileName() + "' belongs to another subject");
            }
            if (!document.isReady()) {
                throw new AppException(ErrorCode.DOCUMENT_NOT_READY,
                        "Document '" + document.getFileName() + "' is " + document.getIndexingStatus());
            }
        }
        return ids;
    }

    private Subject requireSubject(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    private Topic loadTopic(UUID subjectId, UUID topicId) {
        if (topicId == null) {
            return null;
        }
        Topic topic = topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (!Objects.equals(topic.getSubjectId(), subjectId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_IN_SUBJECT);
        }
        return topic;
    }

    /** WF01 step 5: cosine retrieval restricted to the subject and the selected READY documents. */
    private Context retrieveContext(QuestionGenerationRequest request) {
        Subject subject = requireSubject(request.getSubjectId());
        Topic topic = loadTopic(request.getSubjectId(), request.getTopicId());
        String query = joinNonBlank(subject.getSubjectName(),
                topic == null ? null : topic.getTopicName(),
                topic == null ? null : topic.getDescription(),
                request.getLecturerNote());

        List<ChunkSearchResult> chunks;
        try {
            float[] queryVector = embeddingModel.generateEmbedding(query);
            chunks = documentChunkRepository.searchSimilarChunks(request.getSubjectId(), request.getDocumentIds(),
                    queryVector, settings.getMinSimilarity(), settings.getTopK());
        } catch (AiServiceException e) {
            failRequest(request, "Embedding service error: " + e.getMessage());
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "Embedding service failed; generation request " + request.getGenerationRequestId()
                            + " can be retried");
        }
        if (chunks.isEmpty()) {
            failRequest(request, ErrorCode.INSUFFICIENT_CONTEXT.getMessage());
            throw new AppException(ErrorCode.INSUFFICIENT_CONTEXT,
                    "No chunk of the selected documents is similar enough (>= " + settings.getMinSimilarity()
                            + ") to the subject/topic; refine the topic or select other documents");
        }
        transactionRunner.inNewTransaction(() -> {
            QuestionGenerationRequest fresh = generationRequestRepository.findById(request.getGenerationRequestId())
                    .orElseThrow();
            fresh.recordRetrievedContext(chunks.stream().map(ChunkSearchResult::chunkId).toList());
            QuestionGenerationRequest saved = generationRequestRepository.save(fresh);
            request.setRetrievedChunkIds(saved.getRetrievedChunkIds());
            request.setVersion(saved.getVersion());
            request.setUpdatedAt(saved.getUpdatedAt());
        });
        return context(subject, topic, chunks);
    }

    /** Context saved at generation time (regeneration and retries reuse the same chunks). */
    private Context storedContext(QuestionGenerationRequest request) {
        Subject subject = requireSubject(request.getSubjectId());
        Topic topic = request.getTopicId() == null ? null : topicRepository.findById(request.getTopicId()).orElse(null);
        Map<UUID, DocumentChunk> chunks = documentChunkRepository.findAllByIds(request.getRetrievedChunkIds()).stream()
                .collect(Collectors.toMap(DocumentChunk::getChunkId, Function.identity()));
        Set<UUID> documentIds = chunks.values().stream().map(DocumentChunk::getDocumentId).collect(Collectors.toSet());
        Map<UUID, CourseDocument> readyDocuments = courseDocumentRepository.findAllByIds(documentIds).stream()
                .filter(CourseDocument::isReady)
                .filter(document -> Objects.equals(document.getSubjectId(), request.getSubjectId()))
                .collect(Collectors.toMap(CourseDocument::getDocumentId, Function.identity()));

        List<ChunkSearchResult> ordered = new ArrayList<>();
        for (UUID chunkId : request.getRetrievedChunkIds()) {
            DocumentChunk chunk = chunks.get(chunkId);
            CourseDocument document = chunk == null ? null : readyDocuments.get(chunk.getDocumentId());
            if (document != null) {
                ordered.add(ChunkSearchResult.of(chunk.getChunkId(), document.getDocumentId(), document.getFileName(),
                        chunk.getChunkIndex(), null, chunk.getContent(), 0.0));
            }
        }
        if (ordered.isEmpty()) {
            throw new AppException(ErrorCode.INSUFFICIENT_CONTEXT,
                    "The documents used by this generation request are no longer READY");
        }
        return context(subject, topic, ordered);
    }

    private Context context(Subject subject, Topic topic, List<ChunkSearchResult> chunks) {
        Map<String, ChunkSearchResult> byRef = new LinkedHashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkSearchResult chunk = chunks.get(i);
            byRef.put("C" + (i + 1), chunk);
            byRef.put(chunk.chunkId().toString().toLowerCase(), chunk);
        }
        return new Context(subject, topic, chunks, byRef);
    }

    private QuestionGenerationPrompt prompt(Context context, BloomDistribution wanted, String note,
                                            List<String> avoid, String previousContent, String feedback) {
        List<QuestionGenerationPrompt.ContextChunk> contextChunks = new ArrayList<>();
        for (int i = 0; i < context.chunks().size(); i++) {
            ChunkSearchResult chunk = context.chunks().get(i);
            contextChunks.add(new QuestionGenerationPrompt.ContextChunk("C" + (i + 1), chunk.documentName(),
                    chunk.page(), chunk.content()));
        }
        return new QuestionGenerationPrompt(
                context.subject().getSubjectName(),
                context.topic() == null ? null : context.topic().getTopicName(),
                wanted.asMap(),
                note,
                contextChunks,
                avoid,
                previousContent,
                feedback);
    }

    private GenerationResult runAndPersist(QuestionGenerationRequest request, Context context,
                                           BloomDistribution wanted, UUID actorId) {
        List<String> existing = questionVersionRepository.findActiveContentsBySubject(request.getSubjectId());
        List<Set<String>> existingTokens = existing.stream().map(QuestionSimilarity::tokens)
                .collect(Collectors.toCollection(ArrayList::new));
        List<String> avoid = existing.stream().limit(settings.getAvoidListSize()).toList();
        QuestionGenerationPrompt basePrompt = prompt(context, wanted, request.getLecturerNote(), avoid, null, null);

        RunOutcome outcome = runAttempts(request, basePrompt, wanted, context, existingTokens);
        return persistOutcome(request, outcome, actorId);
    }

    /**
     * WF01 steps 6-7 with bounded retries: each attempt asks only for the questions still missing,
     * per Bloom level. {@code request} is null for single-question regeneration (no request budget).
     */
    private RunOutcome runAttempts(QuestionGenerationRequest request, QuestionGenerationPrompt basePrompt,
                                   BloomDistribution wanted, Context context, List<Set<String>> existingTokens) {
        Map<BloomLevel, Integer> missing = new EnumMap<>(wanted.asMap());
        List<ValidatedQuestion> accepted = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        int rejected = 0;
        String lastError = null;

        for (int attempt = 1; attempt <= settings.getAttemptsPerRun() && totalOf(missing) > 0; attempt++) {
            if (request != null) {
                if (request.getAttemptCount() >= settings.getMaxTotalAttempts()) {
                    issues.add("Attempt budget of " + settings.getMaxTotalAttempts() + " exhausted");
                    break;
                }
                request.beginAttempt(settings.getMaxTotalAttempts());
            }
            QuestionGenerationPrompt prompt = withDistribution(basePrompt, missing);
            List<GeneratedQuestionCandidate> candidates;
            try {
                candidates = questionGenerator.generate(prompt);
            } catch (AiServiceException e) {
                lastError = "LLM error: " + e.getMessage();
                issues.add("Attempt " + attempt + ": " + lastError);
                continue;
            }
            if (candidates == null || candidates.isEmpty()) {
                lastError = "LLM returned no question";
                issues.add("Attempt " + attempt + ": " + lastError);
                continue;
            }
            int index = 0;
            for (GeneratedQuestionCandidate candidate : candidates) {
                index++;
                Set<BloomLevel> stillNeeded = missing.entrySet().stream()
                        .filter(e -> e.getValue() > 0)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                Outcome result = GeneratedQuestionValidator.validate(candidate, context.byRef(), stillNeeded,
                        existingTokens, settings.getDuplicateThreshold());
                if (result.valid()) {
                    ValidatedQuestion question = result.question();
                    accepted.add(question);
                    missing.merge(question.bloomLevel(), -1, Integer::sum);
                    existingTokens.add(QuestionSimilarity.tokens(question.content()));
                } else {
                    rejected++;
                    issues.add("Attempt " + attempt + ", item " + index + ": " + String.join("; ", result.reasons()));
                }
            }
        }
        return new RunOutcome(accepted, rejected, issues.size() > MAX_ISSUES ? issues.subList(0, MAX_ISSUES) : issues,
                lastError);
    }

    private GenerationResult persistOutcome(QuestionGenerationRequest request, RunOutcome outcome, UUID actorId) {
        try {
            return transactionRunner.inNewTransaction(() -> {
                // Lost assignment during the LLM call → nothing is saved (WF01 exception flow).
                subjectAccessGuard.requireManage(actorId, request.getSubjectId());
                QuestionGenerationRequest fresh = generationRequestRepository.findById(request.getGenerationRequestId())
                        .orElseThrow(() -> new AppException(ErrorCode.GENERATION_REQUEST_NOT_FOUND));
                fresh.setAttemptCount(request.getAttemptCount());

                List<NewDraft> drafts = outcome.accepted().stream()
                        .map(question -> toDraft(fresh, question, actorId))
                        .toList();
                draftWriter.persist(drafts);

                String error = null;
                if (drafts.isEmpty()) {
                    error = outcome.lastError() != null ? outcome.lastError() : "Every generated candidate failed validation";
                }
                fresh.recordOutcome(drafts.size(), outcome.rejected(), outcome.issues(), error);
                QuestionGenerationRequest saved = generationRequestRepository.save(fresh);

                Map<String, Object> audit = new HashMap<>();
                audit.put("subjectId", saved.getSubjectId());
                audit.put("status", saved.getStatus());
                audit.put("accepted", drafts.size());
                audit.put("rejected", outcome.rejected());
                audit.put("attempts", saved.getAttemptCount());
                auditor.record(actorId, "QUESTIONS_GENERATED", QuestionBankAuditor.GENERATION_REQUEST,
                        saved.getGenerationRequestId(), audit);

                List<QuestionVersion> versions = questionVersionRepository.findAllByIds(
                        drafts.stream().map(d -> d.version().getQuestionVersionId()).toList());
                return new GenerationResult(saved, viewAssembler.assemble(versions));
            });
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.FORBIDDEN_SUBJECT) {
                failRequest(request, "Permission on the subject was revoked during generation");
            }
            throw e;
        }
    }

    private NewDraft toDraft(QuestionGenerationRequest request, ValidatedQuestion validated, UUID actorId) {
        Question question = Question.newDraftOwner(request.getSubjectId(), request.getTopicId(), actorId);
        QuestionVersion version = QuestionVersion.newDraft(question.getQuestionId(), 1, validated.content(),
                validated.expectedAnswer(), validated.bloomLevel(), QuestionGenerationMode.AI_RAG, actorId);
        version.setGenerationRequestId(request.getGenerationRequestId());
        Rubric rubric = Rubric.create(version.getQuestionVersionId(), null, null);
        List<RubricCriterion> criteria = toCriteria(rubric, validated);
        rubric.recalculateTotal(criteria);
        return new NewDraft(question, version, rubric, criteria, toSources(version.getQuestionVersionId(), validated));
    }

    private void replaceRubricAndSources(QuestionVersion version, ValidatedQuestion validated) {
        UUID versionId = version.getQuestionVersionId();
        Rubric rubric = rubricRepository.findByQuestionVersionId(versionId)
                .orElseGet(() -> Rubric.create(versionId, null, null));
        rubricCriterionRepository.deleteByRubricId(rubric.getRubricId());
        List<RubricCriterion> criteria = toCriteria(rubric, validated);
        rubric.recalculateTotal(criteria);
        rubricRepository.save(rubric);
        rubricCriterionRepository.saveAll(criteria);
        questionSourceRepository.deleteByQuestionVersionId(versionId);
        questionSourceRepository.saveAll(toSources(versionId, validated));
    }

    private static List<RubricCriterion> toCriteria(Rubric rubric, ValidatedQuestion validated) {
        List<RubricCriterion> criteria = new ArrayList<>();
        for (int i = 0; i < validated.criteria().size(); i++) {
            GeneratedQuestionValidator.ValidatedCriterion criterion = validated.criteria().get(i);
            criteria.add(RubricCriterion.create(rubric.getRubricId(), criterion.name(), criterion.description(),
                    criterion.maxScore(), List.of(), i + 1));
        }
        return criteria;
    }

    private static List<QuestionSource> toSources(UUID versionId, ValidatedQuestion validated) {
        List<QuestionSource> sources = new ArrayList<>();
        int order = 1;
        for (ValidatedSource source : validated.sources()) {
            sources.add(QuestionSource.builder()
                    .questionSourceId(UUID.randomUUID())
                    .questionVersionId(versionId)
                    .chunkId(source.chunk().chunkId())
                    .documentId(source.chunk().documentId())
                    .documentName(source.chunk().documentName())
                    .citationQuote(source.quote())
                    .similarityScore(source.chunk().similarityScore() > 0
                            ? BigDecimal.valueOf(source.chunk().similarityScore()).setScale(3, RoundingMode.HALF_UP)
                            : null)
                    .sourceOrder(order++)
                    .createdAt(java.time.Instant.now())
                    .build());
        }
        return sources;
    }

    private void failRequest(QuestionGenerationRequest request, String error) {
        try {
            transactionRunner.inNewTransaction(() -> generationRequestRepository.findById(request.getGenerationRequestId())
                    .ifPresent(fresh -> {
                        fresh.setAttemptCount(Math.max(fresh.getAttemptCount(), request.getAttemptCount()));
                        fresh.markFailed(error);
                        generationRequestRepository.save(fresh);
                    }));
        } catch (Exception e) {
            log.error("Could not mark generation request {} as FAILED", request.getGenerationRequestId(), e);
        }
    }

    private static QuestionGenerationPrompt withDistribution(QuestionGenerationPrompt base, Map<BloomLevel, Integer> missing) {
        Map<BloomLevel, Integer> wanted = new EnumMap<>(BloomLevel.class);
        missing.forEach((level, count) -> {
            if (count > 0) {
                wanted.put(level, count);
            }
        });
        return new QuestionGenerationPrompt(base.subjectName(), base.topicName(), wanted, base.lecturerNote(),
                base.contextChunks(), base.avoidQuestions(), base.previousContent(), base.lecturerFeedback());
    }

    private static int totalOf(Map<BloomLevel, Integer> counts) {
        return counts.values().stream().mapToInt(v -> Math.max(0, v)).sum();
    }

    private static String joinNonBlank(String... parts) {
        return java.util.Arrays.stream(parts)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(". "));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
