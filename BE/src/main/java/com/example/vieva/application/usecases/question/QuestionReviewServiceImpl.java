package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.QuestionVersionSearchCriteria;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectHeadService;
import com.example.vieva.application.usecases.question.QuestionAccessLoader.VersionContext;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.exception.FieldViolation;
import com.example.vieva.domain.services.QuestionApprovalPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionReviewServiceImpl implements QuestionReviewService {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuestionSourceRepository questionSourceRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final CourseDocumentRepository courseDocumentRepository;
    private final QuestionAccessLoader accessLoader;
    private final QuestionVersionViewAssembler viewAssembler;
    private final QuestionBankAuditor auditor;
    private final SubjectHeadService heads;
    private final ReviewNotifications notifications;
    private final PostgreSQLDatabase db;

    @Override
    @Transactional
    public QuestionVersionView submitForReview(UUID versionId, Long expectedVersion, UUID actorId) {
        VersionContext ctx = accessLoader.versionForWrite(versionId, actorId);
        ctx.question().ensureActive();
        ctx.version().ensureVersionMatches(expectedVersion);
        ctx.version().ensureDraft();
        UUID head = heads.headOf(ctx.question().getSubjectId());
        validateForReview(ctx.version());
        ctx.version().submitForReview();
        QuestionVersion submitted = db.updateStatus(ctx.version());
        notifications.notifyPendingReview(head, ctx.question().getQuestionId(), versionId);
        auditor.record(actorId, "QUESTION_SUBMITTED", QuestionBankAuditor.QUESTION_VERSION, versionId,
                Map.of("questionId", ctx.question().getQuestionId()));
        return db.fetchQuestionWithRubric(submitted);
    }

    @Override
    @Transactional
    public QuestionVersionView reviewQuestion(UUID versionId, UUID actorId) {
        VersionContext ctx = accessLoader.versionForReview(versionId, actorId);
        heads.requireHead(actorId, ctx.question().getSubjectId());
        ctx.version().ensureUnderReview();
        return db.fetchQuestionWithRubric(ctx.version());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<QuestionVersionView> searchVersions(QuestionVersionSearchCriteria criteria) {
        PagedResult<QuestionVersion> page = questionVersionRepository.search(criteria);
        return PagedResult.of(viewAssembler.assemble(page.getContent()), page.getPage(), page.getSize(),
                page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionVersionView getVersion(UUID versionId) {
        QuestionVersion version = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND));
        return viewAssembler.assemble(version);
    }

    /**
     * WF01 step 9 in one atomic transaction (BR-08): re-check permission, state, content, Bloom
     * confirmation, rubric totals and RAG sources; then publish the version and supersede the old one.
     */
    @Override
    @Transactional
    public QuestionVersionView approve(UUID versionId, Long expectedVersion, UUID actorId) {
        VersionContext ctx = accessLoader.versionForReview(versionId, actorId);
        QuestionVersion version = ctx.version();
        Question question = ctx.question();
        version.ensureVersionMatches(expectedVersion);
        heads.requireHead(actorId, question.getSubjectId());
        version.ensureUnderReview();
        question.ensureActive();
        Rubric rubric = validateForReview(version);

        UUID previousApprovedId = question.getCurrentApprovedVersionId();
        version.approve(actorId);
        db.updateStatus(version);

        if (previousApprovedId != null && !previousApprovedId.equals(versionId)) {
            questionVersionRepository.findById(previousApprovedId).ifPresent(previous -> {
                previous.supersede();
                    db.updateStatus(previous);
            });
        }
        question.publish(versionId);
        questionRepository.save(question);
        notifications.notifyQuestionApproved(version.getCreatedBy(), question.getQuestionId(), versionId);
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("questionId", question.getQuestionId());
        newValues.put("versionNumber", version.getVersionNumber());
        newValues.put("supersededVersionId", previousApprovedId);
        newValues.put("totalScore", rubric.getTotalPoints());
        auditor.record(actorId, "QUESTION_APPROVED", QuestionBankAuditor.QUESTION_VERSION, versionId, newValues);
        return viewAssembler.assemble(questionVersionRepository.findById(versionId).orElseThrow());
    }

    private Rubric validateForReview(QuestionVersion version) {
        UUID versionId = version.getQuestionVersionId();
        Rubric rubric = rubricRepository.findByQuestionVersionId(versionId).orElse(null);
        List<RubricCriterion> criteria = rubric == null ? List.of()
                : rubricCriterionRepository.findByRubricId(rubric.getRubricId());
        List<QuestionSource> sources = questionSourceRepository.findByQuestionVersionId(versionId);
        Map<UUID, DocumentChunk> chunks = loadChunks(sources);
        Set<UUID> readyDocuments = loadReadyDocumentIds(chunks.values());

        List<FieldViolation> violations = QuestionApprovalPolicy.validate(version, rubric, criteria, sources,
                chunks, readyDocuments);
        if (!violations.isEmpty()) {
            throw AppException.ofViolations(violations);
        }

        return rubric;
    }

    @Override
    @Transactional
    public QuestionVersionView reject(UUID versionId, String reason, Long expectedVersion, UUID actorId) {
        VersionContext ctx = accessLoader.versionForReview(versionId, actorId);
        QuestionVersion version = ctx.version();
        heads.requireHead(actorId, ctx.question().getSubjectId());
        ctx.question().ensureActive();
        version.ensureVersionMatches(expectedVersion);
        version.reject(actorId, reason);
        db.updateStatus(version);
        auditor.record(actorId, "QUESTION_REJECTED", QuestionBankAuditor.QUESTION_VERSION, versionId,
                Map.of("questionId", ctx.question().getQuestionId(), "reason", version.getRejectionReason()));
        return viewAssembler.assemble(questionVersionRepository.findById(versionId).orElseThrow());
    }

    private Map<UUID, DocumentChunk> loadChunks(List<QuestionSource> sources) {
        Set<UUID> chunkIds = sources.stream()
                .map(QuestionSource::getChunkId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return documentChunkRepository.findAllByIds(chunkIds).stream()
                .collect(Collectors.toMap(DocumentChunk::getChunkId, Function.identity(), (a, b) -> a));
    }

    private Set<UUID> loadReadyDocumentIds(java.util.Collection<DocumentChunk> chunks) {
        Set<UUID> documentIds = chunks.stream().map(DocumentChunk::getDocumentId).collect(Collectors.toSet());
        return courseDocumentRepository.findAllByIds(documentIds).stream()
                .filter(CourseDocument::isAvailableAsEvidence)
                .map(CourseDocument::getDocumentId)
                .collect(Collectors.toSet());
    }
}
