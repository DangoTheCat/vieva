package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.input.RubricInput;
import com.example.vieva.application.ports.output.QuestionVersionView;

import java.util.UUID;

/**
 * UC1.7: rubric of a DRAFT version. The rubric of an APPROVED version is immutable (edit it through
 * Copy-on-Write). Every change recomputes total = Σ max scores (BR-02).
 */
public interface RubricService {
    QuestionVersionView getRubric(UUID versionId);

    QuestionVersionView replaceRubric(UUID versionId, RubricInput input, Long expectedVersion, UUID actorId);

    QuestionVersionView addCriterion(UUID versionId, CriterionInput input, Long expectedVersion, UUID actorId);

    QuestionVersionView updateCriterion(UUID versionId, UUID criterionId, CriterionInput input, Long expectedVersion,
                                        UUID actorId);

    QuestionVersionView deleteCriterion(UUID versionId, UUID criterionId, Long expectedVersion, UUID actorId);
}
