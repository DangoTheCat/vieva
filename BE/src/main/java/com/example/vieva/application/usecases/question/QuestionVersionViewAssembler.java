package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.entities.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds {@link QuestionVersionView}s with a fixed number of batched queries (no N+1).
 */
@Component
@RequiredArgsConstructor
public class QuestionVersionViewAssembler {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuestionSourceRepository questionSourceRepository;

    public QuestionVersionView assemble(QuestionVersion version) {
        return assemble(List.of(version)).get(0);
    }

    public List<QuestionVersionView> assemble(List<QuestionVersion> versions) {
        if (versions == null || versions.isEmpty()) {
            return List.of();
        }
        Set<UUID> questionIds = versions.stream().map(QuestionVersion::getQuestionId).collect(Collectors.toSet());
        Map<UUID, Question> questions = byId(questionRepository.findAllByIds(questionIds), Question::getQuestionId);
        return assemble(versions, questions);
    }

    /** Variant for callers that already hold the questions. */
    public List<QuestionVersionView> assemble(List<QuestionVersion> versions, Map<UUID, Question> questions) {
        if (versions == null || versions.isEmpty()) {
            return List.of();
        }
        Set<UUID> versionIds = versions.stream().map(QuestionVersion::getQuestionVersionId).collect(Collectors.toSet());
        Set<UUID> topicIds = questions.values().stream()
                .map(Question::getTopicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Topic> topics = byId(topicRepository.findAllByIds(topicIds), Topic::getTopicId);
        Map<UUID, Rubric> rubricsByVersion = byId(rubricRepository.findByQuestionVersionIds(versionIds),
                Rubric::getQuestionVersionId);
        Set<UUID> rubricIds = rubricsByVersion.values().stream().map(Rubric::getRubricId).collect(Collectors.toSet());
        Map<UUID, List<RubricCriterion>> criteriaByRubric = rubricCriterionRepository.findByRubricIds(rubricIds).stream()
                .collect(Collectors.groupingBy(RubricCriterion::getRubricId));
        Map<UUID, List<QuestionSource>> sourcesByVersion = questionSourceRepository.findByQuestionVersionIds(versionIds)
                .stream()
                .collect(Collectors.groupingBy(QuestionSource::getQuestionVersionId));

        return versions.stream().map(version -> {
            Question question = questions.get(version.getQuestionId());
            Rubric rubric = rubricsByVersion.get(version.getQuestionVersionId());
            List<RubricCriterion> criteria = rubric == null ? List.of()
                    : sortCriteria(criteriaByRubric.getOrDefault(rubric.getRubricId(), List.of()));
            List<QuestionSource> sources = sourcesByVersion.getOrDefault(version.getQuestionVersionId(), List.of()).stream()
                    .sorted((a, b) -> Integer.compare(orderOf(a.getSourceOrder()), orderOf(b.getSourceOrder())))
                    .collect(Collectors.toList());
            return QuestionVersionView.builder()
                    .question(question)
                    .topic(question == null || question.getTopicId() == null ? null : topics.get(question.getTopicId()))
                    .version(version)
                    .rubric(rubric)
                    .criteria(criteria)
                    .sources(sources)
                    .build();
        }).collect(Collectors.toList());
    }

    static List<RubricCriterion> sortCriteria(Collection<RubricCriterion> criteria) {
        return criteria.stream()
                .sorted((a, b) -> Integer.compare(orderOf(a.getOrderIndex()), orderOf(b.getOrderIndex())))
                .collect(Collectors.toList());
    }

    private static int orderOf(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private static <T> Map<UUID, T> byId(Collection<T> items, Function<T, UUID> key) {
        return items.stream().collect(Collectors.toMap(key, Function.identity(), (first, second) -> first));
    }
}
