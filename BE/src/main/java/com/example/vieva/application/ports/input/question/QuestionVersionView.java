package com.example.vieva.application.ports.input.question;

import com.example.vieva.application.ports.input.catalog.TopicResult;
import lombok.Builder;

import java.util.List;

/**
 * One question version view with domain-free data: owning question, topic, rubric, criteria, sources.
 */
@Builder
public record QuestionVersionView(
        QuestionResult question,
        TopicResult topic,
        QuestionVersionResult version,
        RubricResult rubric,
        List<RubricCriterionResult> criteria,
        List<QuestionSourceResult> sources
) {
    public String topicName() {
        return topic == null ? null : topic.getTopicName();
    }
}

