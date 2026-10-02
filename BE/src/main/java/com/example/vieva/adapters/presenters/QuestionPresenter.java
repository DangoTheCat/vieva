package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionDetailView;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class QuestionPresenter {

    public QuestionDetailDto toDto(QuestionDetailView view) {
        if (view == null) {
            return null;
        }

        Question question = view.getQuestion();
        return QuestionDetailDto.builder()
                .questionId(question != null ? question.getQuestionId() : null)
                .topicId(question != null ? question.getTopicId() : null)
                .questionCode(question != null ? question.getQuestionCode() : null)
                .status(question != null ? question.getStatus() : null)
                .hasPendingDraft(view.isHasPendingDraft())
                .activeVersion(toVersionDto(view.getActiveVersion()))
                .draftVersion(toVersionDto(view.getDraftVersion()))
                .rubric(toRubricDto(view.getRubric(), view.getCriteria()))
                .sources(toSourceDtoList(view.getSources()))
                .versionHistory(toVersionDtoList(view.getVersionHistory()))
                .createdAt(question != null ? question.getCreatedAt() : null)
                .updatedAt(question != null ? question.getUpdatedAt() : null)
                .build();
    }

    public List<QuestionDetailDto> toDtoList(List<QuestionDetailView> views) {
        if (views == null) {
            return Collections.emptyList();
        }
        return views.stream().map(this::toDto).collect(Collectors.toList());
    }

    public PageResponse<QuestionDetailDto> toPageResponse(PagedResult<QuestionDetailView> pagedResult) {
        if (pagedResult == null) {
            return null;
        }
        return PageResponse.<QuestionDetailDto>builder()
                .content(toDtoList(pagedResult.getContent()))
                .page(pagedResult.getPage())
                .size(pagedResult.getSize())
                .totalElements(pagedResult.getTotalElements())
                .totalPages(pagedResult.getTotalPages())
                .isFirst(pagedResult.isFirst())
                .isLast(pagedResult.isLast())
                .build();
    }

    public QuestionVersionDto toVersionDto(QuestionVersion version) {
        if (version == null) {
            return null;
        }
        return QuestionVersionDto.builder()
                .versionId(version.getQuestionVersionId())
                .questionId(version.getQuestionId())
                .versionNumber(version.getVersionNumber())
                .questionContent(version.getQuestionContent())
                .referenceAnswer(version.getReferenceAnswer())
                .bloomLevel(version.getBloomLevel())
                .generationMode(version.getGenerationMode())
                .approvalStatus(version.getApprovalStatus())
                .rejectionReason(version.getRejectionReason())
                .reviewedBy(version.getReviewedBy())
                .reviewedAt(version.getReviewedAt())
                .createdAt(version.getCreatedAt())
                .build();
    }

    public List<QuestionVersionDto> toVersionDtoList(List<QuestionVersion> versions) {
        if (versions == null) {
            return Collections.emptyList();
        }
        return versions.stream().map(this::toVersionDto).collect(Collectors.toList());
    }

    public RubricDto toRubricDto(Rubric rubric, List<RubricCriterion> criteria) {
        if (rubric == null) {
            return null;
        }
        return RubricDto.builder()
                .rubricId(rubric.getRubricId())
                .rubricName(rubric.getRubricName())
                .totalPoints(rubric.getTotalPoints())
                .description(rubric.getDescription())
                .criteria(toCriterionDtoList(criteria))
                .build();
    }

    public List<RubricCriterionDto> toCriterionDtoList(List<RubricCriterion> criteria) {
        if (criteria == null) {
            return Collections.emptyList();
        }
        return criteria.stream()
                .map(c -> RubricCriterionDto.builder()
                        .criterionId(c.getCriterionId())
                        .criterionName(c.getCriterionName())
                        .maxPoints(c.getMaxPoints())
                        .achievementDescriptors(c.getAchievementDescriptors())
                        .orderIndex(c.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
    }

    public List<QuestionSourceDto> toSourceDtoList(List<QuestionSource> sources) {
        if (sources == null) {
            return Collections.emptyList();
        }
        return sources.stream()
                .map(s -> QuestionSourceDto.builder()
                        .questionSourceId(s.getQuestionSourceId())
                        .chunkId(s.getChunkId())
                        .documentName(s.getDocumentName())
                        .citationQuote(s.getCitationQuote())
                        .similarityScore(s.getSimilarityScore())
                        .build())
                .collect(Collectors.toList());
    }
}
