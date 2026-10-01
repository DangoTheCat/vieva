package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Khung Rubric đánh giá; quan hệ 1:1 với QuestionVersion; APPROVED bắt buộc có Rubric hợp lệ.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rubric {
    private UUID rubricId;
    private UUID questionVersionId;
    private String rubricName;
    private BigDecimal totalPoints;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
