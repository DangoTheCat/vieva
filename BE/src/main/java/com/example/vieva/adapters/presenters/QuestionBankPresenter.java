package com.example.vieva.adapters.presenters;

import com.example.vieva.adapters.presenters.QuestionBankDtos.GenerationRequestDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.PerformanceLevelDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionBankItemDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionDetailDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionSourceDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.QuestionVersionDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.RubricCriterionDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.RubricDto;
import com.example.vieva.adapters.presenters.QuestionBankDtos.TopicDto;
import com.example.vieva.application.ports.input.catalog.TopicResult;
import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.input.generation.GenerationRequestResult;
import com.example.vieva.application.ports.input.generation.GenerationResult;
import com.example.vieva.application.ports.input.question.PerformanceLevelResult;
import com.example.vieva.application.ports.input.question.QuestionBankItemView;
import com.example.vieva.application.ports.input.question.QuestionDetailView;
import com.example.vieva.application.ports.input.question.QuestionResult;
import com.example.vieva.application.ports.input.question.QuestionSourceResult;
import com.example.vieva.application.ports.input.question.QuestionVersionResult;
import com.example.vieva.application.ports.input.question.QuestionVersionView;
import com.example.vieva.application.ports.input.question.RubricCriterionResult;
import com.example.vieva.application.ports.input.question.RubricResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionBankPresenter {

    public QuestionVersionDto toDto(QuestionVersionView view) {
        if (view == null) {
            return null;
        }
        return summary(view.version(), view.question(), view.topicName()).toBuilder()
                .rubric(toRubric(view.rubric()))
                .sources(view.sources() == null ? List.of() : view.sources().stream().map(this::toSource).toList())
                .build();
    }

    /** Version fields only (history lists, pending draft reference). */
    public QuestionVersionDto summary(QuestionVersionResult version, QuestionResult question, String topicName) {
        if (version == null) {
            return null;
        }
        return QuestionVersionDto.builder()
                .versionId(version.versionId())
                .questionId(version.questionId())
                .questionCode(question == null ? null : question.questionCode())
                .subjectId(question == null ? null : question.subjectId())
                .topicId(question == null ? null : question.topicId())
                .topicName(topicName)
                .versionNumber(version.versionNumber())
                .content(version.questionContent())
                .expectedAnswer(version.referenceAnswer())
                .bloomLevel(version.bloomLevel())
                .bloomLevelLabel(version.bloomLevelLabel())
                .bloomConfirmed(version.bloomConfirmed())
                .origin(version.origin())
                .status(version.status())
                .parentVersionId(version.parentVersionId())
                .generationRequestId(version.generationRequestId())
                .regenerationCount(version.regenerationCount())
                .rejectReason(version.rejectReason())
                .createdBy(version.createdBy())
                .reviewedBy(version.reviewedBy())
                .reviewedAt(version.reviewedAt())
                .createdAt(version.createdAt())
                .updatedAt(version.updatedAt())
                .lockVersion(version.lockVersion())
                .build();
    }

    public List<QuestionVersionDto> toDtoList(List<QuestionVersionView> views) {
        return views == null ? List.of() : views.stream().map(this::toDto).toList();
    }

    public PageResponse<QuestionVersionDto> toVersionPage(PageResult<QuestionVersionView> page) {
        return PageResponse.from(page, this::toDto);
    }

    public PageResponse<QuestionBankItemDto> toBankPage(PageResult<QuestionBankItemView> page) {
        return PageResponse.from(page, this::toBankItem);
    }

    public QuestionBankItemDto toBankItem(QuestionBankItemView item) {
        QuestionVersionView current = item.current();
        QuestionResult question = current.question();
        return QuestionBankItemDto.builder()
                .questionId(question.questionId())
                .questionCode(question.questionCode())
                .subjectId(question.subjectId())
                .topicId(question.topicId())
                .topicName(current.topicName())
                .status(question.status())
                .hasPendingDraft(item.hasPendingDraft())
                .currentVersion(toDto(current))
                .createdAt(question.createdAt())
                .updatedAt(question.updatedAt())
                .build();
    }

    public QuestionDetailDto toDetail(QuestionDetailView detail) {
        QuestionResult question = detail.question();
        String topicName = detail.currentVersion() == null ? null : detail.currentVersion().topicName();
        return QuestionDetailDto.builder()
                .questionId(question.questionId())
                .questionCode(question.questionCode())
                .subjectId(question.subjectId())
                .topicId(question.topicId())
                .status(question.status())
                .currentApprovedVersionId(question.currentApprovedVersionId())
                .currentVersion(toDto(detail.currentVersion()))
                .pendingDraft(summary(detail.pendingDraft(), question, topicName))
                .history(detail.history() == null ? List.of()
                        : detail.history().stream().map(v -> summary(v, question, topicName)).toList())
                .createdAt(question.createdAt())
                .updatedAt(question.updatedAt())
                .build();
    }

    public GenerationRequestDto toGeneration(GenerationResult result) {
        GenerationRequestResult request = result.request();
        return GenerationRequestDto.builder()
                .generationRequestId(request.generationRequestId())
                .subjectId(request.subjectId())
                .topicId(request.topicId())
                .documentIds(request.documentIds())
                .bloomDistribution(request.bloomDistribution())
                .totalQuestions(request.totalQuestions())
                .lecturerNote(request.lecturerNote())
                .status(request.status())
                .generatedCount(request.generatedCount())
                .rejectedCount(request.rejectedCount())
                .attemptCount(request.attemptCount())
                .issues(request.issues())
                .errorMessage(request.errorMessage())
                .createdAt(request.createdAt())
                .updatedAt(request.updatedAt())
                .drafts(toDtoList(result.drafts()))
                .build();
    }

    public TopicDto toTopic(TopicResult topic) {
        if (topic == null) {
            return null;
        }
        return TopicDto.builder()
                .topicId(topic.getTopicId())
                .subjectId(topic.getSubjectId())
                .name(topic.getTopicName())
                .description(topic.getDescription())
                .orderIndex(topic.getOrderIndex())
                .build();
    }

    private RubricDto toRubric(RubricResult rubric) {
        if (rubric == null) {
            return null;
        }
        return RubricDto.builder()
                .rubricId(rubric.rubricId())
                .name(rubric.name())
                .description(rubric.description())
                .totalScore(rubric.totalScore())
                .criteria(rubric.criteria() == null ? List.of() : rubric.criteria().stream()
                        .map(this::toCriterion)
                        .toList())
                .build();
    }

    private RubricCriterionDto toCriterion(RubricCriterionResult c) {
        return RubricCriterionDto.builder()
                .criterionId(c.criterionId())
                .name(c.name())
                .description(c.description())
                .maxScore(c.maxScore())
                .levels(c.levels() == null ? List.of() : c.levels().stream().map(this::toLevel).toList())
                .orderIndex(c.orderIndex())
                .build();
    }

    private PerformanceLevelDto toLevel(PerformanceLevelResult l) {
        return PerformanceLevelDto.builder()
                .label(l.label())
                .description(l.description())
                .score(l.score())
                .build();
    }

    private QuestionSourceDto toSource(QuestionSourceResult source) {
        return QuestionSourceDto.builder()
                .sourceId(source.sourceId())
                .chunkId(source.chunkId())
                .documentId(source.documentId())
                .documentName(source.documentName())
                .citationQuote(source.citationQuote())
                .similarityScore(source.similarityScore())
                .order(source.order())
                .build();
    }
}
