package com.example.vieva.application.usecases.question;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.exception.FieldViolation;
import com.example.vieva.application.ports.input.question.CreateRubricTemplateCommand;
import com.example.vieva.application.ports.input.question.CriterionInput;
import com.example.vieva.application.ports.input.question.PerformanceLevelResult;
import com.example.vieva.application.ports.input.question.RubricCriterionResult;
import com.example.vieva.application.ports.input.question.RubricInput;
import com.example.vieva.application.ports.input.question.RubricResult;
import com.example.vieva.application.ports.input.question.RubricTemplateResult;
import com.example.vieva.application.ports.input.question.RubricTemplateService;
import com.example.vieva.application.ports.input.question.UpdateRubricTemplateCommand;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.RubricTemplateRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.question.RubricDraftFactory.RubricDraft;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.entities.RubricTemplate;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@UseCaseTransaction
public class RubricTemplateServiceImpl implements RubricTemplateService {

    private final RubricTemplateRepository rubricTemplateRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionRepository questionRepository;
    private final JsonSerializerPort jsonSerializer;
    private final SubjectAccessGuard accessGuard;
    private final QuestionBankAuditor auditor;

    public RubricTemplateServiceImpl(
            RubricTemplateRepository rubricTemplateRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            QuestionVersionRepository questionVersionRepository,
            SubjectAccessGuard accessGuard,
            QuestionBankAuditor auditor
    ) {
        this(rubricTemplateRepository, rubricRepository, rubricCriterionRepository, questionVersionRepository, null, null, accessGuard, auditor);
    }

    @Override
    public RubricTemplateResult createTemplate(CreateRubricTemplateCommand cmd, UUID actorId) {
        if (cmd.getSubjectId() == null || cmd.getName() == null || cmd.getName().isBlank()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        accessGuard.requireManage(actorId, cmd.getSubjectId());

        Instant now = Instant.now();
        RubricTemplate template = RubricTemplate.builder()
                .id(UUID.randomUUID())
                .subjectId(cmd.getSubjectId())
                .name(cmd.getName().trim())
                .description(cmd.getDescription() != null ? cmd.getDescription().trim() : null)
                .criteriaJson(cmd.getCriteriaJson() != null ? cmd.getCriteriaJson() : "[]")
                .createdAt(now)
                .updatedAt(now)
                .build();

        RubricTemplate saved = rubricTemplateRepository.save(template);
        auditor.record(actorId, "RUBRIC_TEMPLATE_CREATED", "RUBRIC_TEMPLATE", saved.getId(),
                Map.of("subjectId", saved.getSubjectId(), "name", saved.getName()));

        return toResult(saved);
    }

    @Override
    public RubricTemplateResult updateTemplate(UUID templateId, UpdateRubricTemplateCommand cmd, UUID actorId) {
        RubricTemplate template = rubricTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RUBRIC_TEMPLATE_NOT_FOUND));

        accessGuard.requireManage(actorId, template.getSubjectId());

        template.update(cmd.getName(), cmd.getDescription(), cmd.getCriteriaJson());
        RubricTemplate saved = rubricTemplateRepository.save(template);

        auditor.record(actorId, "RUBRIC_TEMPLATE_UPDATED", "RUBRIC_TEMPLATE", saved.getId(),
                Map.of("name", saved.getName()));

        return toResult(saved);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public RubricTemplateResult getTemplateById(UUID templateId) {
        return rubricTemplateRepository.findById(templateId)
                .map(this::toResult)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RUBRIC_TEMPLATE_NOT_FOUND));
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<RubricTemplateResult> getTemplatesBySubject(UUID subjectId) {
        return rubricTemplateRepository.findBySubjectId(subjectId).stream()
                .map(this::toResult)
                .toList();
    }

    @Override
    public void deleteTemplate(UUID templateId, UUID actorId) {
        RubricTemplate template = rubricTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RUBRIC_TEMPLATE_NOT_FOUND));

        accessGuard.requireManage(actorId, template.getSubjectId());
        rubricTemplateRepository.deleteById(templateId);

        auditor.record(actorId, "RUBRIC_TEMPLATE_DELETED", "RUBRIC_TEMPLATE", templateId,
                Map.of("subjectId", template.getSubjectId()));
    }

    @Override
    public RubricResult copyTemplateToQuestionVersion(UUID templateId, UUID versionId, UUID actorId) {
        RubricTemplate template = rubricTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RUBRIC_TEMPLATE_NOT_FOUND));

        QuestionVersion version = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_VERSION_NOT_FOUND));

        if (!version.isDraft()) {
            throw new ApplicationException(ErrorCode.VERSION_NOT_DRAFT);
        }

        if (questionRepository != null && version.getQuestionId() != null) {
            Question question = questionRepository.findById(version.getQuestionId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_NOT_FOUND));

            if (!template.getSubjectId().equals(question.getSubjectId())) {
                throw new ApplicationException(ErrorCode.FORBIDDEN_SUBJECT, "Template subject does not match question subject");
            }
        }

        accessGuard.requireManage(actorId, template.getSubjectId());

        CriterionInput[] parsedCriteria = null;
        if (jsonSerializer != null && template.getCriteriaJson() != null && !template.getCriteriaJson().isBlank()) {
            try {
                parsedCriteria = jsonSerializer.deserialize(template.getCriteriaJson(), CriterionInput[].class);
            } catch (Exception ignored) {
                // Keep parsedCriteria null to fall through to validation
            }
        }

        List<CriterionInput> criteriaList = (parsedCriteria != null && parsedCriteria.length > 0)
                ? Arrays.asList(parsedCriteria)
                : List.of();

        if (criteriaList.isEmpty()) {
            throw new ApplicationException(ErrorCode.RUBRIC_REQUIRED, "Template has no criteria");
        }

        BigDecimal totalScore = criteriaList.stream()
                .map(CriterionInput::maxScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RubricInput rubricInput = new RubricInput(
                template.getName(),
                template.getDescription(),
                totalScore,
                criteriaList
        );

        List<FieldViolation> violations = new ArrayList<>();
        RubricDraft draft = RubricDraftFactory.build(versionId, rubricInput, violations);
        if (draft == null || !violations.isEmpty()) {
            throw ApplicationException.ofViolations(violations);
        }

        Rubric savedRubric = RubricDraftFactory.replace(versionId, draft, rubricRepository, rubricCriterionRepository);
        List<RubricCriterion> savedCriteria = rubricCriterionRepository.findByRubricId(savedRubric.getRubricId());

        return RubricResult.builder()
                .rubricId(savedRubric.getRubricId())
                .name(savedRubric.getRubricName())
                .description(savedRubric.getDescription())
                .totalScore(savedRubric.getTotalPoints())
                .criteria(savedCriteria.stream().map(c -> {
                    List<PerformanceLevelResult> levelResults = c.getPerformanceLevels() != null
                            ? c.getPerformanceLevels().stream()
                                    .map(l -> new PerformanceLevelResult(l.label(), l.description(), l.score()))
                                    .toList()
                            : List.of();
                    return RubricCriterionResult.builder()
                            .criterionId(c.getCriterionId())
                            .name(c.getCriterionName())
                            .description(c.getAchievementDescriptors())
                            .maxScore(c.getMaxPoints())
                            .levels(levelResults)
                            .orderIndex(c.getOrderIndex())
                            .build();
                }).toList())
                .build();
    }

    private RubricTemplateResult toResult(RubricTemplate t) {
        return RubricTemplateResult.builder()
                .id(t.getId())
                .subjectId(t.getSubjectId())
                .name(t.getName())
                .description(t.getDescription())
                .criteriaJson(t.getCriteriaJson())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
