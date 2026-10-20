package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.output.QuestionVersionViewAssemblerPort;
import com.example.vieva.application.ports.input.question.CreateManualQuestionCommand;
import com.example.vieva.application.ports.input.question.UpdateDraftCommand;
import com.example.vieva.application.ports.input.question.QuestionAuthoringService;
import com.example.vieva.application.usecases.common.EnumValues;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.question.QuestionAccessLoader.VersionContext;
import com.example.vieva.application.ports.output.QuestionDraftWriter;
import com.example.vieva.application.ports.output.QuestionDraftWriter.NewDraft;
import com.example.vieva.application.usecases.question.RubricDraftFactory.RubricDraft;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.exception.FieldViolation;
import lombok.RequiredArgsConstructor;
import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.application.transaction.UseCaseTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class QuestionAuthoringServiceImpl implements QuestionAuthoringService {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuestionSourceRepository questionSourceRepository;
    private final TopicRepository topicRepository;
    private final SubjectAccessGuard subjectAccessGuard;
    private final QuestionAccessLoader accessLoader;
    private final QuestionDraftWriter draftWriter;
    private final QuestionVersionViewAssemblerPort viewAssembler;
    private final QuestionBankAuditor auditor;

    @Override
    @UseCaseTransaction
    public QuestionVersionView createManualQuestion(UUID subjectId, CreateManualQuestionCommand command, UUID actorId) {
        subjectAccessGuard.requireManage(actorId, subjectId);
        validateTopic(subjectId, command.topicId());

        List<FieldViolation> violations = new ArrayList<>();
        requireText(command.content(), "content", violations);
        requireText(command.expectedAnswer(), "expectedAnswer", violations);
        BloomLevel bloomLevel = null;
        if (command.bloomLevel() != null && !command.bloomLevel().isBlank()) {
            bloomLevel = EnumValues.parse(BloomLevel.class, command.bloomLevel(), "Malformed request body");
        }
        if (bloomLevel == null) {
            violations.add(FieldViolation.of("bloomLevel", ErrorCode.INVALID_REQUEST, "Bloom level is required"));
        }

        Question question = Question.newDraftOwner(subjectId, command.topicId(), actorId);
        QuestionVersion version = QuestionVersion.newDraft(question.getQuestionId(), 1,
                trim(command.content()), trim(command.expectedAnswer()), bloomLevel,
                QuestionGenerationMode.MANUAL, actorId);
        RubricDraft rubric = RubricDraftFactory.build(version.getQuestionVersionId(), command.rubric(), violations);
        if (!violations.isEmpty()) {
            throw ApplicationException.ofViolations(violations);
        }

        draftWriter.saveQuestionDraft(List.of(new NewDraft(question, version, rubric.rubric(), rubric.criteria(), List.of())));
        auditor.record(actorId, "QUESTION_CREATED", QuestionBankAuditor.QUESTION_VERSION, version.getQuestionVersionId(),
                Map.of("questionId", question.getQuestionId(), "origin", QuestionGenerationMode.MANUAL,
                        "bloomLevel", version.getBloomLevel()));
        return reloadView(version.getQuestionVersionId());
    }

    @Override
    @UseCaseTransaction
    public QuestionVersionView updateDraft(UUID versionId, UpdateDraftCommand command, UUID actorId) {
        VersionContext ctx = accessLoader.versionForWrite(versionId, actorId);
        QuestionVersion version = ctx.version();
        Question question = ctx.question();
        version.ensureVersionMatches(command.expectedVersion());
        version.ensureDraft();
        question.ensureActive();

        List<FieldViolation> violations = new ArrayList<>();
        if (command.content() != null && command.content().isBlank()) {
            violations.add(FieldViolation.of("content", ErrorCode.CONTENT_REQUIRED, "Question content must not be empty"));
        }
        if (command.expectedAnswer() != null && command.expectedAnswer().isBlank()) {
            violations.add(FieldViolation.of("expectedAnswer", ErrorCode.CONTENT_REQUIRED, "Expected answer must not be empty"));
        }
        RubricDraft newRubric = command.rubric() == null ? null
                : RubricDraftFactory.build(versionId, command.rubric(), violations);
        if (!violations.isEmpty()) {
            throw ApplicationException.ofViolations(violations);
        }

        Map<String, Object> before = snapshot(version);
        if (command.topicId() != null && !command.topicId().equals(question.getTopicId())) {
            validateTopic(question.getSubjectId(), command.topicId());
            question.setTopicId(command.topicId());
            question.setUpdatedAt(java.time.Instant.now());
            questionRepository.save(question);
        }
        BloomLevel bloomLevel = null;
        if (command.bloomLevel() != null && !command.bloomLevel().isBlank()) {
            bloomLevel = EnumValues.parse(BloomLevel.class, command.bloomLevel(), "Malformed request body");
        }
        version.editContent(command.content(), command.expectedAnswer(), bloomLevel, command.bloomConfirmed());
        if (newRubric != null) {
            RubricDraftFactory.replace(versionId, newRubric, rubricRepository, rubricCriterionRepository);
        }
        questionVersionRepository.save(version);

        auditor.record(actorId, "QUESTION_DRAFT_UPDATED", QuestionBankAuditor.QUESTION_VERSION, versionId,
                before, snapshot(version));
        return reloadView(versionId);
    }

    @Override
    @UseCaseTransaction
    public QuestionVersionView confirmBloom(UUID versionId, String bloomLevel, Long expectedVersion, UUID actorId) {
        VersionContext ctx = accessLoader.versionForWrite(versionId, actorId);
        QuestionVersion version = ctx.version();
        version.ensureVersionMatches(expectedVersion);
        ctx.question().ensureActive();
        BloomLevel parsed = EnumValues.parse(BloomLevel.class, bloomLevel, "Malformed request body");
        BloomLevel previous = version.getBloomLevel();
        version.confirmBloom(parsed);
        questionVersionRepository.save(version);
        auditor.record(actorId, "QUESTION_BLOOM_CONFIRMED", QuestionBankAuditor.QUESTION_VERSION, versionId,
                Map.of("bloomLevel", previous == null ? "" : previous),
                Map.of("bloomLevel", version.getBloomLevel(), "bloomConfirmed", true));
        return reloadView(versionId);
    }

    @Override
    @UseCaseTransaction
    public void deleteDraft(UUID versionId, UUID actorId) {
        VersionContext ctx = accessLoader.versionForWrite(versionId, actorId);
        QuestionVersion version = ctx.version();
        version.ensureDraft();

        rubricRepository.findByQuestionVersionId(versionId).ifPresent(rubric -> {
            rubricCriterionRepository.deleteByRubricId(rubric.getRubricId());
            rubricRepository.deleteByQuestionVersionId(versionId);
        });
        questionSourceRepository.deleteByQuestionVersionId(versionId);
        questionVersionRepository.deleteById(versionId);

        // A draft-only question disappears with its last draft; published questions keep their history.
        boolean orphan = questionVersionRepository.findByQuestionId(ctx.question().getQuestionId()).isEmpty();
        if (orphan) {
            questionRepository.deleteById(ctx.question().getQuestionId());
        }
        auditor.record(actorId, "QUESTION_DRAFT_DELETED", QuestionBankAuditor.QUESTION_VERSION, versionId,
                snapshot(version), Map.of("questionDeleted", orphan));
    }

    @Override
    @UseCaseTransaction
    public QuestionVersionView createDraftFromApproved(UUID questionId, UUID actorId) {
        Question question = accessLoader.questionForWrite(questionId, actorId);
        question.ensureActive();
        if (question.getCurrentApprovedVersionId() == null) {
            throw new ApplicationException(ErrorCode.NO_APPROVED_VERSION);
        }
        if (questionVersionRepository.hasDraftVersion(questionId)) {
            throw new ApplicationException(ErrorCode.DRAFT_ALREADY_EXISTS);
        }
        QuestionVersion approved = questionVersionRepository.findById(question.getCurrentApprovedVersionId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_VERSION_NOT_FOUND));
        int nextNumber = questionVersionRepository.findLatestVersion(questionId)
                .map(QuestionVersion::getVersionNumber)
                .orElse(approved.getVersionNumber()) + 1;

        QuestionVersion draft = approved.copyAsDraft(nextNumber, actorId);
        try {
            questionVersionRepository.save(draft);
        } catch (DuplicateKeyException e) {
            // Concurrent request won the race on uq_question_draft_per_question / uq_question_version.
            throw new ApplicationException(ErrorCode.DRAFT_ALREADY_EXISTS);
        }

        rubricRepository.findByQuestionVersionId(approved.getQuestionVersionId()).ifPresent(rubric -> {
            Rubric copy = rubric.copyTo(draft.getQuestionVersionId());
            rubricRepository.save(copy);
            List<RubricCriterion> criteria = rubricCriterionRepository.findByRubricId(rubric.getRubricId()).stream()
                    .map(criterion -> criterion.copyTo(copy.getRubricId()))
                    .toList();
            rubricCriterionRepository.saveAll(criteria);
        });
        List<QuestionSource> sources = questionSourceRepository.findByQuestionVersionId(approved.getQuestionVersionId())
                .stream()
                .map(source -> source.copyTo(draft.getQuestionVersionId()))
                .toList();
        questionSourceRepository.saveAll(sources);

        auditor.record(actorId, "QUESTION_VERSION_CREATED", QuestionBankAuditor.QUESTION_VERSION, draft.getQuestionVersionId(),
                Map.of("questionId", questionId, "parentVersionId", approved.getQuestionVersionId(),
                        "versionNumber", nextNumber));
        return reloadView(draft.getQuestionVersionId());
    }

    private void validateTopic(UUID subjectId, UUID topicId) {
        if (topicId == null) {
            return;
        }
        if (topicRepository.findById(topicId).isEmpty()) {
            throw new ApplicationException(ErrorCode.TOPIC_NOT_FOUND);
        }
        if (!topicRepository.existsByIdAndSubjectId(topicId, subjectId)) {
            throw new ApplicationException(ErrorCode.TOPIC_NOT_IN_SUBJECT);
        }
    }

    private QuestionVersionView reloadView(UUID versionId) {
        QuestionVersion fresh = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_VERSION_NOT_FOUND));
        return viewAssembler.assemble(fresh);
    }

    static Map<String, Object> snapshot(QuestionVersion version) {
        Map<String, Object> values = new HashMap<>();
        values.put("content", version.getQuestionContent());
        values.put("expectedAnswer", version.getReferenceAnswer());
        values.put("bloomLevel", version.getBloomLevel());
        values.put("bloomConfirmed", version.isBloomConfirmed());
        values.put("status", version.getApprovalStatus());
        return values;
    }

    private static void requireText(String value, String field, List<FieldViolation> violations) {
        if (value == null || value.isBlank()) {
            violations.add(FieldViolation.of(field, ErrorCode.CONTENT_REQUIRED, field + " must not be empty"));
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
