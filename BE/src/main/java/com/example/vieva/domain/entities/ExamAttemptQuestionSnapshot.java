package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttemptQuestionSnapshot {
    private UUID id;
    private UUID attemptId;
    private UUID questionId;
    private UUID questionVersionId;
    private int orderIndex;
    private String questionText;
    private String expectedAnswer;
    private String rubricSnapshot;
    private Instant createdAt;

    public static ExamAttemptQuestionSnapshot create(UUID attemptId, UUID questionId, UUID questionVersionId,
                                                      int orderIndex, String questionText, String expectedAnswer,
                                                      String rubricSnapshot) {
        return ExamAttemptQuestionSnapshot.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId)
                .questionVersionId(questionVersionId)
                .orderIndex(orderIndex)
                .questionText(questionText)
                .expectedAnswer(expectedAnswer)
                .rubricSnapshot(rubricSnapshot)
                .createdAt(Instant.now())
                .build();
    }
}
