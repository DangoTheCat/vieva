package com.example.vieva.application.usecases.catalog;

import com.example.vieva.application.ports.input.catalog.TopicResult;
import com.example.vieva.domain.entities.Topic;

public final class TopicResults {

    private TopicResults() {
    }

    public static TopicResult from(Topic topic) {
        if (topic == null) {
            return null;
        }
        return TopicResult.builder()
                .topicId(topic.getTopicId())
                .subjectId(topic.getSubjectId())
                .topicName(topic.getTopicName())
                .orderIndex(topic.getOrderIndex())
                .description(topic.getDescription())
                .createdAt(topic.getCreatedAt())
                .build();
    }
}
