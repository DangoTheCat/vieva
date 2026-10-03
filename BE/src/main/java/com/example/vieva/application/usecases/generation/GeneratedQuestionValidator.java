package com.example.vieva.application.usecases.generation;

import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.services.QuestionSimilarity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * WF01 step 7: validates one LLM candidate against the fixed schema and business rules —
 * required fields, Bloom level among the 6 levels and still requested, at least one source taken
 * from the retrieved chunks, verbatim citations, Σ maxScore = totalScore, no near-duplicate.
 */
public final class GeneratedQuestionValidator {

    static final int MAX_CONTENT_LENGTH = 4000;
    static final int EXCERPT_LENGTH = 300;

    private GeneratedQuestionValidator() {
    }

    public record ValidatedSource(ChunkSearchResult chunk, String quote) {
    }

    public record ValidatedCriterion(String name, String description, BigDecimal maxScore) {
    }

    public record ValidatedQuestion(String content, String expectedAnswer, BloomLevel bloomLevel,
                                    BigDecimal totalScore, List<ValidatedCriterion> criteria,
                                    List<ValidatedSource> sources) {
    }

    /** Either {@code question} is set, or {@code reasons} explains why the candidate was rejected. */
    public record Outcome(ValidatedQuestion question, List<String> reasons) {
        public boolean valid() {
            return question != null;
        }
    }

    /**
     * @param contextByRef     retrieved chunks keyed by their prompt ref (C1...) — a source must be one of them
     * @param requestedLevels  Bloom levels still needed in this run
     * @param existingQuestions token sets of questions already in the subject or accepted in this run
     */
    public static Outcome validate(GeneratedQuestionCandidate candidate,
                                   Map<String, ChunkSearchResult> contextByRef,
                                   Set<BloomLevel> requestedLevels,
                                   Collection<Set<String>> existingQuestions,
                                   double duplicateThreshold) {
        List<String> reasons = new ArrayList<>();
        if (candidate == null) {
            return new Outcome(null, List.of("empty item"));
        }

        String content = trim(candidate.content());
        String answer = trim(candidate.expectedAnswer());
        if (content == null) {
            reasons.add("content is missing");
        } else if (content.length() > MAX_CONTENT_LENGTH) {
            reasons.add("content is longer than " + MAX_CONTENT_LENGTH + " characters");
        }
        if (answer == null) {
            reasons.add("expectedAnswer is missing");
        }

        BloomLevel level = BloomLevel.fromCodeOrLabel(candidate.bloomLevel()).orElse(null);
        if (level == null) {
            reasons.add("bloomLevel '" + candidate.bloomLevel() + "' is not one of the 6 Bloom levels");
        } else if (!requestedLevels.contains(level)) {
            reasons.add("bloomLevel " + level + " was not requested (or already filled)");
        }

        List<ValidatedCriterion> criteria = validateRubric(candidate, reasons);
        List<ValidatedSource> sources = validateSources(candidate, contextByRef, reasons);

        if (content != null && QuestionSimilarity.isNearDuplicate(content, existingQuestions, duplicateThreshold)) {
            reasons.add("near-duplicate of an existing question");
        }

        if (!reasons.isEmpty()) {
            return new Outcome(null, reasons);
        }
        return new Outcome(new ValidatedQuestion(content, answer, level, candidate.rubricTotalScore(), criteria, sources),
                List.of());
    }

    private static List<ValidatedCriterion> validateRubric(GeneratedQuestionCandidate candidate, List<String> reasons) {
        List<ValidatedCriterion> criteria = new ArrayList<>();
        if (candidate.criteria() == null || candidate.criteria().isEmpty()) {
            reasons.add("rubric has no criteria");
            return criteria;
        }
        BigDecimal sum = BigDecimal.ZERO;
        boolean criteriaValid = true;
        for (GeneratedQuestionCandidate.Criterion criterion : candidate.criteria()) {
            if (criterion == null || trim(criterion.name()) == null || trim(criterion.description()) == null) {
                reasons.add("rubric criterion without name or description");
                criteriaValid = false;
                continue;
            }
            if (criterion.maxScore() == null || criterion.maxScore().signum() <= 0) {
                reasons.add("rubric criterion '" + criterion.name() + "' has no positive maxScore");
                criteriaValid = false;
                continue;
            }
            sum = sum.add(criterion.maxScore());
            criteria.add(new ValidatedCriterion(criterion.name().trim(), criterion.description().trim(), criterion.maxScore()));
        }
        BigDecimal total = candidate.rubricTotalScore();
        if (total == null || total.signum() <= 0) {
            reasons.add("rubric totalScore is missing");
        } else if (criteriaValid && sum.compareTo(total) != 0) {
            reasons.add("rubric criteria sum (" + sum + ") differs from totalScore (" + total + ")");
        }
        return criteria;
    }

    private static List<ValidatedSource> validateSources(GeneratedQuestionCandidate candidate,
                                                         Map<String, ChunkSearchResult> contextByRef,
                                                         List<String> reasons) {
        Map<String, ValidatedSource> sources = new LinkedHashMap<>();
        List<String> refs = candidate.sourceChunkRefs() == null ? List.of() : candidate.sourceChunkRefs();
        for (String rawRef : refs) {
            String ref = normalizeRef(rawRef);
            ChunkSearchResult chunk = ref == null ? null : contextByRef.get(ref);
            if (chunk == null) {
                reasons.add("source '" + rawRef + "' is not one of the retrieved chunks");
                continue;
            }
            sources.putIfAbsent(ref, null);
        }
        if (sources.isEmpty()) {
            if (refs.isEmpty()) {
                reasons.add("no source chunk cited");
            }
            return List.of();
        }

        List<GeneratedQuestionCandidate.Citation> citations = candidate.citations() == null ? List.of() : candidate.citations();
        for (GeneratedQuestionCandidate.Citation citation : citations) {
            if (citation == null) {
                continue;
            }
            String ref = normalizeRef(citation.chunkRef());
            if (ref == null || !sources.containsKey(ref) || trim(citation.quote()) == null) {
                continue;
            }
            ChunkSearchResult chunk = contextByRef.get(ref);
            if (!QuestionSource.isGroundedIn(citation.quote(), chunk.content())) {
                reasons.add("citation for " + ref + " is not found verbatim in the chunk");
                continue;
            }
            if (sources.get(ref) == null) {
                sources.put(ref, new ValidatedSource(chunk, citation.quote().trim()));
            }
        }
        // Sources without an explicit quote cite the opening of the chunk (always grounded).
        List<ValidatedSource> result = new ArrayList<>();
        sources.forEach((ref, source) -> result.add(source != null ? source
                : new ValidatedSource(contextByRef.get(ref), excerpt(contextByRef.get(ref).content()))));
        return result;
    }

    /** Accepts "C3", "c3", "[C3]" or the chunk UUID itself (the context map holds both keys). */
    static String normalizeRef(String raw) {
        if (raw == null) {
            return null;
        }
        String ref = raw.trim().replace("[", "").replace("]", "");
        if (ref.isEmpty()) {
            return null;
        }
        return ref.length() <= 5 ? ref.toUpperCase() : ref.toLowerCase();
    }

    static String excerpt(String content) {
        String normalized = Objects.requireNonNullElse(content, "").trim();
        if (normalized.length() <= EXCERPT_LENGTH) {
            return normalized;
        }
        int cut = normalized.lastIndexOf(' ', EXCERPT_LENGTH);
        return normalized.substring(0, cut > 0 ? cut : EXCERPT_LENGTH);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
