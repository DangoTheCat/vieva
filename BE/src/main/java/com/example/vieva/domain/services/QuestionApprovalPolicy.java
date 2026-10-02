package com.example.vieva.domain.services;

import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.exception.FieldViolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure business rules checked when a DRAFT version is approved (UC1.3, BR-01/02/03).
 * Returns every violation so the lecturer can fix all fields in one round.
 */
public final class QuestionApprovalPolicy {

    private QuestionApprovalPolicy() {
    }

    /**
     * @param chunksById    source chunks that still exist, keyed by id
     * @param readyDocumentIds documents that are READY and not deleted (sources must point to them)
     */
    public static List<FieldViolation> validate(QuestionVersion version,
                                                Rubric rubric,
                                                Collection<RubricCriterion> criteria,
                                                Collection<QuestionSource> sources,
                                                Map<UUID, DocumentChunk> chunksById,
                                                Set<UUID> readyDocumentIds) {
        List<FieldViolation> violations = new ArrayList<>();

        if (isBlank(version.getQuestionContent())) {
            violations.add(FieldViolation.of("content", ErrorCode.CONTENT_REQUIRED, "Question content must not be empty"));
        }
        if (isBlank(version.getReferenceAnswer())) {
            violations.add(FieldViolation.of("expectedAnswer", ErrorCode.CONTENT_REQUIRED, "Expected answer must not be empty"));
        }

        if (version.getBloomLevel() == null) {
            violations.add(FieldViolation.of("bloomLevel", ErrorCode.BLOOM_NOT_CONFIRMED, "Bloom level is required"));
        } else if (!version.isBloomConfirmed()) {
            violations.add(FieldViolation.of("bloomConfirmed", ErrorCode.BLOOM_NOT_CONFIRMED,
                    "Bloom level " + version.getBloomLevel() + " must be confirmed by the lecturer"));
        }

        validateRubric(rubric, criteria, violations);

        if (version.isAiGenerated()) {
            validateSources(sources, chunksById, readyDocumentIds, violations);
        }
        return violations;
    }

    /** BR-02 checks shared by approval and LLM-output validation. */
    public static void validateRubric(Rubric rubric, Collection<RubricCriterion> criteria, List<FieldViolation> out) {
        if (rubric == null || criteria == null || criteria.isEmpty()) {
            out.add(FieldViolation.of("rubric.criteria", ErrorCode.RUBRIC_REQUIRED,
                    "Rubric must have at least one criterion"));
            return;
        }
        int index = 0;
        for (RubricCriterion criterion : criteria) {
            if (criterion.getMaxPoints() == null || criterion.getMaxPoints().signum() <= 0) {
                out.add(FieldViolation.of("rubric.criteria[" + index + "].maxScore", ErrorCode.RUBRIC_SCORE_MISMATCH,
                        "Criterion max score must be greater than 0"));
            }
            if (isBlank(criterion.getCriterionName())) {
                out.add(FieldViolation.of("rubric.criteria[" + index + "].name", ErrorCode.RUBRIC_REQUIRED,
                        "Criterion name must not be empty"));
            }
            index++;
        }
        if (!rubric.totalMatches(criteria)) {
            out.add(FieldViolation.of("rubric.totalScore", ErrorCode.RUBRIC_SCORE_MISMATCH,
                    "Sum of criteria max scores (" + Rubric.sumOf(criteria) + ") must equal total score ("
                            + rubric.getTotalPoints() + ")"));
        }
    }

    private static void validateSources(Collection<QuestionSource> sources,
                                        Map<UUID, DocumentChunk> chunksById,
                                        Set<UUID> readyDocumentIds,
                                        List<FieldViolation> out) {
        if (sources == null || sources.isEmpty()) {
            out.add(FieldViolation.of("sources", ErrorCode.SOURCE_REQUIRED,
                    "AI-generated question must cite at least one source chunk"));
            return;
        }
        int index = 0;
        for (QuestionSource source : sources) {
            String field = "sources[" + index++ + "]";
            DocumentChunk chunk = source.getChunkId() == null ? null : chunksById.get(source.getChunkId());
            if (chunk == null) {
                out.add(FieldViolation.of(field, ErrorCode.SOURCE_REQUIRED, "Source chunk no longer exists"));
                continue;
            }
            if (!readyDocumentIds.contains(chunk.getDocumentId())) {
                out.add(FieldViolation.of(field, ErrorCode.SOURCE_REQUIRED,
                        "Source document is no longer READY or was deleted"));
                continue;
            }
            if (!source.isGroundedIn(chunk.getContent())) {
                out.add(FieldViolation.of(field + ".citationQuote", ErrorCode.INVALID_CITATION_QUOTE,
                        "Citation quote is not found in the source chunk"));
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
