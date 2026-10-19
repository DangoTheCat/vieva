package com.example.vieva.application.ports.input.catalog;

import com.example.vieva.domain.entities.Topic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResult {
    private UUID topicId;
    private UUID subjectId;
    private String topicName;
    private Integer orderIndex;
    private String description;
    private Instant createdAt;

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
