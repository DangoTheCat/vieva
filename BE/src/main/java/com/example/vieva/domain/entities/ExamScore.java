package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExamScore {
    private UUID id;
    private UUID attemptId;
    private BigDecimal totalScore;
    private GradeLevel grade;
    private String detailedFeedback;
    private ScoringMode scoringMode;
    private Instant createdAt;

    public static ExamScore create(UUID attemptId, BigDecimal totalScore, GradeLevel grade,
                                   String detailedFeedback, ScoringMode scoringMode) {
        return ExamScore.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .totalScore(totalScore)
                .grade(grade)
                .detailedFeedback(detailedFeedback)
                .scoringMode(scoringMode)
                .createdAt(Instant.now())
                .build();
    }
}
