package com.example.vieva.application.ports.input.catalog;

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
}
