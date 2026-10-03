package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.input.RubricInput;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.question.QuestionAccessLoader.VersionContext;
import com.example.vieva.application.usecases.question.RubricDraftFactory.RubricDraft;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.exception.FieldViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RubricServiceImpl implements RubricService {

    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuestionAccessLoader accessLoader;
    private final QuestionVersionViewAssembler viewAssembler;
    private final QuestionBankAuditor auditor;

    @Override
    @Transactional(readOnly = true)
    public QuestionVersionView getRubric(UUID versionId) {
        QuestionVersion version = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND));
        return viewAssembler.assemble(version);
    }

    @Override
    @Transactional
    public QuestionVersionView replaceRubric(UUID versionId, RubricInput input, Long expectedVersion, UUID actorId) {
        QuestionVersion version = draftForWrite(versionId, expectedVersion, actorId);
        List<FieldViolation> violations = new ArrayList<>();
        RubricDraft draft = RubricDraftFactory.build(versionId, input, violations);
        if (!violations.isEmpty()) {
            throw AppException.ofViolations(violations);
        }
        BigDecimal before = currentTotal(versionId);

        Rubric rubric = rubricRepository.findByQuestionVersionId(versionId).orElse(null);
        if (rubric == null) {
            rubric = draft.rubric();
        } else {
            rubricCriterionRepository.deleteByRubricId(rubric.getRubricId());
            rubric.rename(draft.rubric().getRubricName(), draft.rubric().getDescription());
        }
        UUID rubricId = rubric.getRubricId();
        List<RubricCriterion> criteria = draft.criteria();
        criteria.forEach(criterion -> criterion.setRubricId(rubricId));
        rubric.recalculateTotal(criteria);
        rubricRepository.save(rubric);
        rubricCriterionRepository.saveAll(criteria);
        return finish(version, actorId, before, rubric);
    }

    @Override
    @Transactional
    public QuestionVersionView addCriterion(UUID versionId, CriterionInput input, Long expectedVersion, UUID actorId) {
        QuestionVersion version = draftForWrite(versionId, expectedVersion, actorId);
        BigDecimal before = currentTotal(versionId);
        Rubric rubric = rubricRepository.findByQuestionVersionId(versionId)
                .orElseGet(() -> rubricRepository.save(Rubric.create(versionId, null, null)));
        List<RubricCriterion> criteria = new ArrayList<>(rubricCriterionRepository.findByRubricId(rubric.getRubricId()));

        List<FieldViolation> violations = new ArrayList<>();
        RubricCriterion criterion = RubricDraftFactory.buildCriterion(rubric.getRubricId(), input,
                criteria.size() + 1, "criterion", violations);
        if (criterion == null) {
            throw AppException.ofViolations(violations);
        }
        criterion.setOrderIndex(criteria.size() + 1);
        criteria.add(criterion);
        rubricCriterionRepository.save(criterion);
        rubric.recalculateTotal(criteria);
        rubricRepository.save(rubric);
        return finish(version, actorId, before, rubric);
    }

    @Override
    @Transactional
    public QuestionVersionView updateCriterion(UUID versionId, UUID criterionId, CriterionInput input,
                                               Long expectedVersion, UUID actorId) {
        QuestionVersion version = draftForWrite(versionId, expectedVersion, actorId);
        BigDecimal before = currentTotal(versionId);
        Rubric rubric = rubricOf(versionId);
        List<RubricCriterion> criteria = rubricCriterionRepository.findByRubricId(rubric.getRubricId());
        RubricCriterion target = criteria.stream()
                .filter(c -> c.getCriterionId().equals(criterionId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CRITERION_NOT_FOUND));

        List<FieldViolation> violations = new ArrayList<>();
        int currentOrder = target.getOrderIndex() == null ? 1 : target.getOrderIndex();
        RubricCriterion validated = RubricDraftFactory.buildCriterion(rubric.getRubricId(), input,
                currentOrder, "criterion", violations);
        if (validated == null) {
            throw AppException.ofViolations(violations);
        }
        target.update(validated.getCriterionName(), validated.getAchievementDescriptors(), validated.getMaxPoints(),
                validated.getPerformanceLevels());
        target.setOrderIndex(validated.getOrderIndex());
        rubricCriterionRepository.save(target);
        rubric.recalculateTotal(criteria);
        rubricRepository.save(rubric);
        return finish(version, actorId, before, rubric);
    }

    @Override
    @Transactional
    public QuestionVersionView deleteCriterion(UUID versionId, UUID criterionId, Long expectedVersion, UUID actorId) {
        QuestionVersion version = draftForWrite(versionId, expectedVersion, actorId);
        BigDecimal before = currentTotal(versionId);
        Rubric rubric = rubricOf(versionId);
        List<RubricCriterion> criteria = new ArrayList<>(rubricCriterionRepository.findByRubricId(rubric.getRubricId()));
        boolean removed = criteria.removeIf(c -> c.getCriterionId().equals(criterionId));
        if (!removed) {
            throw new AppException(ErrorCode.CRITERION_NOT_FOUND);
        }
        rubricCriterionRepository.deleteById(criterionId);
        // Keep order indexes contiguous after a removal.
        for (int i = 0; i < criteria.size(); i++) {
            criteria.get(i).setOrderIndex(i + 1);
        }
        rubricCriterionRepository.saveAll(criteria);
        rubric.recalculateTotal(criteria);
        rubricRepository.save(rubric);
        return finish(version, actorId, before, rubric);
    }

    private QuestionVersion draftForWrite(UUID versionId, Long expectedVersion, UUID actorId) {
        VersionContext ctx = accessLoader.versionForWrite(versionId, actorId);
        QuestionVersion version = ctx.version();
        version.ensureVersionMatches(expectedVersion);
        // APPROVED rubrics are immutable: edits must go through Copy-on-Write.
        version.ensureDraft();
        ctx.question().ensureActive();
        return version;
    }

    private Rubric rubricOf(UUID versionId) {
        return rubricRepository.findByQuestionVersionId(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.CRITERION_NOT_FOUND, "Version has no rubric yet"));
    }

    private BigDecimal currentTotal(UUID versionId) {
        return rubricRepository.findByQuestionVersionId(versionId).map(Rubric::getTotalPoints).orElse(BigDecimal.ZERO);
    }

    /** Touches the version so concurrent edit/approve of the same draft conflict on the lock (BR-08). */
    private QuestionVersionView finish(QuestionVersion version, UUID actorId, BigDecimal before, Rubric rubric) {
        version.touch();
        questionVersionRepository.save(version);
        auditor.record(actorId, "RUBRIC_UPDATED", QuestionBankAuditor.QUESTION_VERSION, version.getQuestionVersionId(),
                Map.of("totalScore", before), Map.of("totalScore", rubric.getTotalPoints()));
        return viewAssembler.assemble(questionVersionRepository.findById(version.getQuestionVersionId()).orElseThrow());
    }
}
