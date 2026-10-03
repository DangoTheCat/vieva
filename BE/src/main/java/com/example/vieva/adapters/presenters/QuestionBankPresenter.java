package com.example.vieva.adapters.presenters;

import com.example.vieva.adapters.presenters.QuestionBankDtos.GenerationRequestDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionBankItemDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionDetailDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionSourceDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionVersionDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.RubricCriterionDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.RubricDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.TopicDto;
import com.example.vieva.application.ports.output.GenerationResult;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionBankItemView;
import com.example.vieva.application.ports.output.QuestionDetailView;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.entities.Topic;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class QuestionBankPresenter {

    public QuestionVersionDto toDto(QuestionVersionView view) {
        if (view == null) {
            return null;
        }
        return summary(view.version(), view.question(), view.topic()).toBuilder()
                .rubric(toRubric(view.rubric(), view.criteria()))
                .sources(view.sources() == null ? List.of() : view.sources().stream().map(this::toSource).toList())
                .build();
    }

    /** Version fields only (history lists, pending draft reference). */
    public QuestionVersionDto summary(QuestionVersion version, Question question, Topic topic) {
        if (version == null) {
            return null;
        }
        return QuestionVersionDto.builder()
                .versionId(version.getQuestionVersionId())
                .questionId(version.getQuestionId())
                .questionCode(question == null ? null : question.getQuestionCode())
                .subjectId(question == null ? null : question.getSubjectId())
                .topicId(question == null ? null : question.getTopicId())
                .topicName(topic == null ? null : topic.getTopicName())
                .versionNumber(version.getVersionNumber())
                .content(version.getQuestionContent())
                .expectedAnswer(version.getReferenceAnswer())
                .bloomLevel(version.getBloomLevel())
                .bloomLevelLabel(version.getBloomLevel() == null ? null : version.getBloomLevel().getViLabel())
                .bloomConfirmed(version.isBloomConfirmed())
                .origin(version.getGenerationMode())
                .status(version.getApprovalStatus())
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

    public List<QuestionVersionDto> toDtoList(List<QuestionVersionView> views) {
        return views == null ? List.of() : views.stream().map(this::toDto).toList();
    }

    public PageResponse<QuestionVersionDto> toVersionPage(PagedResult<QuestionVersionView> page) {
        return toPage(page, this::toDto);
    }

    public PageResponse<QuestionBankItemDto> toBankPage(PagedResult<QuestionBankItemView> page) {
        return toPage(page, this::toBankItem);
    }

    public QuestionBankItemDto toBankItem(QuestionBankItemView item) {
        QuestionVersionView current = item.current();
        Question question = current.question();
        return QuestionBankItemDto.builder()
                .questionId(question.getQuestionId())
                .questionCode(question.getQuestionCode())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .topicName(current.topic() == null ? null : current.topic().getTopicName())
                .status(question.getStatus())
                .hasPendingDraft(item.hasPendingDraft())
                .currentVersion(toDto(current))
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    public QuestionDetailDto toDetail(QuestionDetailView detail) {
        Question question = detail.question();
        Topic topic = detail.currentVersion() == null ? null : detail.currentVersion().topic();
        return QuestionDetailDto.builder()
                .questionId(question.getQuestionId())
                .questionCode(question.getQuestionCode())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .status(question.getStatus())
                .currentApprovedVersionId(question.getCurrentApprovedVersionId())
                .currentVersion(toDto(detail.currentVersion()))
                .pendingDraft(summary(detail.pendingDraft(), question, topic))
                .history(detail.history() == null ? List.of()
                        : detail.history().stream().map(v -> summary(v, question, topic)).toList())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    public GenerationRequestDto toGeneration(GenerationResult result) {
        QuestionGenerationRequest request = result.request();
        return GenerationRequestDto.builder()
                .generationRequestId(request.getGenerationRequestId())
                .subjectId(request.getSubjectId())
                .topicId(request.getTopicId())
                .documentIds(request.getDocumentIds())
                .bloomDistribution(request.getBloomDistribution())
                .totalQuestions(request.getTotalQuestions())
                .lecturerNote(request.getLecturerNote())
                .status(request.getStatus())
                .generatedCount(request.getGeneratedCount())
                .rejectedCount(request.getRejectedCount())
                .attemptCount(request.getAttemptCount())
                .issues(request.getIssues())
                .errorMessage(request.getErrorMessage())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .drafts(toDtoList(result.drafts()))
                .build();
    }

    public TopicDto toTopic(Topic topic) {
        return TopicDto.builder()
                .topicId(topic.getTopicId())
                .subjectId(topic.getSubjectId())
                .name(topic.getTopicName())
                .description(topic.getDescription())
                .orderIndex(topic.getOrderIndex())
                .build();
    }

    private RubricDto toRubric(Rubric rubric, List<RubricCriterion> criteria) {
        if (rubric == null) {
            return null;
        }
        return RubricDto.builder()
                .rubricId(rubric.getRubricId())
                .name(rubric.getRubricName())
                .description(rubric.getDescription())
                .totalScore(rubric.getTotalPoints())
                .criteria(criteria == null ? List.of() : criteria.stream()
                        .map(c -> RubricCriterionDto.builder()
                                .criterionId(c.getCriterionId())
                                .name(c.getCriterionName())
                                .description(c.getAchievementDescriptors())
                                .maxScore(c.getMaxPoints())
                                .levels(c.getPerformanceLevels() == null ? List.of() : c.getPerformanceLevels())
                                .orderIndex(c.getOrderIndex())
                                .build())
                        .toList())
                .build();
    }

    private QuestionSourceDto toSource(QuestionSource source) {
        return QuestionSourceDto.builder()
                .sourceId(source.getQuestionSourceId())
                .chunkId(source.getChunkId())
                .documentId(source.getDocumentId())
                .documentName(source.getDocumentName())
                .citationQuote(source.getCitationQuote())
                .similarityScore(source.getSimilarityScore())
                .order(source.getSourceOrder())
                .build();
    }

    private static <T, R> PageResponse<R> toPage(PagedResult<T> page, Function<T, R> mapper) {
        return PageResponse.<R>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getPage())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
