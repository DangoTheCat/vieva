package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Chủ đề kiến thức thuộc một môn học cụ thể.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic {
    private UUID topicId;
    private UUID subjectId;
    private String topicName;
    private Integer orderIndex;
    private String description;
    private Instant createdAt;
}
