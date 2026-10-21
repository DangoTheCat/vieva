package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.catalog.TopicResult;
import com.example.vieva.application.ports.input.generation.GenerationRequestResult;
import com.example.vieva.application.ports.input.question.PerformanceLevelResult;
import com.example.vieva.application.ports.input.question.QuestionResult;
import com.example.vieva.application.ports.input.question.QuestionSourceResult;
import com.example.vieva.application.ports.input.question.QuestionVersionResult;
import com.example.vieva.application.ports.input.question.QuestionVersionView;
import com.example.vieva.application.ports.input.question.RubricCriterionResult;
import com.example.vieva.application.ports.input.question.RubricResult;
import com.example.vieva.application.ports.output.QuestionVersionAggregate;
import com.example.vieva.application.usecases.catalog.TopicResults;
import com.example.vieva.domain.entities.PerformanceLevel;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestionViewMapper {

    private QuestionViewMapper() {
    }

    public static QuestionResult toResult(Question question) {
        if (question == null) {
            return null;
        }
        return QuestionResult.builder()
                .questionId(question.getQuestionId())
                .questionCode(question.getQuestionCode())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .status(question.getStatus() == null ? null : question.getStatus().name())
                .currentApprovedVersionId(question.getCurrentApprovedVersionId())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    public static QuestionVersionResult toResult(QuestionVersion version) {
        if (version == null) {
            return null;
        }
        return QuestionVersionResult.builder()
                .versionId(version.getQuestionVersionId())
                .questionId(version.getQuestionId())
                .versionNumber(version.getVersionNumber())
                .questionContent(version.getQuestionContent())
                .referenceAnswer(version.getReferenceAnswer())
                .bloomLevel(version.getBloomLevel() == null ? null : version.getBloomLevel().name())
                .bloomLevelLabel(version.getBloomLevel() == null ? null : version.getBloomLevel().getViLabel())
                .bloomConfirmed(version.isBloomConfirmed())
                .origin(version.getGenerationMode() == null ? null : version.getGenerationMode().name())
                .status(version.getApprovalStatus() == null ? null : version.getApprovalStatus().name())
                .parentVersionId(version.getParentVersionId())
                .generationRequestId(version.getGenerationRequestId())
                .regenerationCount(version.getRegenerationCount())
                .rejectReason(version.getRejectionReason())
                .createdBy(version.getCreatedBy())
                .reviewedBy(version.getReviewedBy())
                .reviewedAt(version.getReviewedAt())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .lockVersion(version.getVersion())
                .build();
    }

    public static PerformanceLevelResult toResult(PerformanceLevel level) {
        if (level == null) {
            return null;
        }
        return new PerformanceLevelResult(level.label(), level.description(), level.score());
    }

    public static RubricCriterionResult toResult(RubricCriterion criterion) {
        if (criterion == null) {
            return null;
        }
        List<PerformanceLevelResult> levels = criterion.getPerformanceLevels() == null ? List.of()
                : criterion.getPerformanceLevels().stream().map(QuestionViewMapper::toResult).toList();
        return RubricCriterionResult.builder()
                .criterionId(criterion.getCriterionId())
                .name(criterion.getCriterionName())
                .description(criterion.getAchievementDescriptors())
                .maxScore(criterion.getMaxPoints())
                .levels(levels)
                .orderIndex(criterion.getOrderIndex())
                .build();
    }

    public static RubricResult toResult(Rubric rubric, List<RubricCriterion> criteria) {
        if (rubric == null) {
            return null;
        }
        List<RubricCriterionResult> criteriaResults = criteria == null ? List.of()
                : criteria.stream().map(QuestionViewMapper::toResult).toList();
        return RubricResult.builder()
                .rubricId(rubric.getRubricId())
                .name(rubric.getRubricName())
                .description(rubric.getDescription())
                .totalScore(rubric.getTotalPoints())
                .criteria(criteriaResults)
                .build();
    }

    public static QuestionSourceResult toResult(QuestionSource source) {
        if (source == null) {
            return null;
        }
        return QuestionSourceResult.builder()
                .sourceId(source.getQuestionSourceId())
                .chunkId(source.getChunkId())
                .documentId(source.getDocumentId())
                .documentName(source.getDocumentName())
                .citationQuote(source.getCitationQuote())
                .similarityScore(source.getSimilarityScore())
                .order(source.getSourceOrder())
                .build();
    }

    public static QuestionVersionView toView(QuestionVersionAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        List<RubricCriterionResult> criteriaResults = aggregate.criteria() == null ? List.of()
                : aggregate.criteria().stream().map(QuestionViewMapper::toResult).toList();
        RubricResult rubricResult = aggregate.rubric() == null ? null
                : toResult(aggregate.rubric(), aggregate.criteria());
        List<QuestionSourceResult> sourceResults = aggregate.sources() == null ? List.of()
                : aggregate.sources().stream().map(QuestionViewMapper::toResult).toList();
        TopicResult topicResult = TopicResults.from(aggregate.topic());

        return QuestionVersionView.builder()
                .question(toResult(aggregate.question()))
                .topic(topicResult)
                .version(toResult(aggregate.version()))
                .rubric(rubricResult)
                .criteria(criteriaResults)
                .sources(sourceResults)
                .build();
    }

    public static List<QuestionVersionView> toViews(List<QuestionVersionAggregate> aggregates) {
        if (aggregates == null) {
            return List.of();
        }
        return aggregates.stream().map(QuestionViewMapper::toView).toList();
    }

    public static GenerationRequestResult toResult(QuestionGenerationRequest request) {
        if (request == null) {
            return null;
        }
        Map<String, Integer> dist = new LinkedHashMap<>();
        if (request.getBloomDistribution() != null) {
            request.getBloomDistribution().forEach((k, v) -> dist.put(k.name(), v));
        }
        return GenerationRequestResult.builder()
                .generationRequestId(request.getGenerationRequestId())
                .subjectId(request.getSubjectId())
                .topicId(request.getTopicId())
                .documentIds(request.getDocumentIds() == null ? List.of() : request.getDocumentIds())
                .retrievedChunkIds(request.getRetrievedChunkIds() == null ? List.of() : request.getRetrievedChunkIds())
                .bloomDistribution(dist)
                .totalQuestions(request.getTotalQuestions())
                .lecturerNote(request.getLecturerNote())
                .status(request.getStatus() == null ? null : request.getStatus().name())
                .generatedCount(request.getGeneratedCount())
                .rejectedCount(request.getRejectedCount())
                .attemptCount(request.getAttemptCount())
                .issues(request.getIssues() == null ? List.of() : request.getIssues())
                .errorMessage(request.getErrorMessage())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
