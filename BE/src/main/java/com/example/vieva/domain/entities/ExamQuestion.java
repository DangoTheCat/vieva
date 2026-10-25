package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestion {
    private UUID id;
    private UUID examId;
    private UUID questionId;
    private UUID questionVersionId;
    private int orderIndex;
    private Instant createdAt;

    public static ExamQuestion create(UUID examId, UUID questionId, UUID questionVersionId, int orderIndex) {
        return ExamQuestion.builder()
                .id(UUID.randomUUID())
                .examId(examId)
                .questionId(questionId)
                .questionVersionId(questionVersionId)
                .orderIndex(orderIndex)
                .createdAt(Instant.now())
                .build();
    }
}
