package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExamTurn {
    private UUID id;
    private UUID attemptId;
    private UUID questionId;
    private UUID questionVersionId;
    private TurnType turnType;
    private UUID parentTurnId;
    private Integer turnOrder;
    private String questionText;
    private String ttsAudioUrl;
    private String studentAudioUrl;
    private String transcriptText;
    private TurnStatus status;
    private BigDecimal score;
    private String feedback;
    private String rubricEvaluations;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExamTurn createMainQuestion(UUID attemptId, UUID questionId, UUID questionVersionId,
                                              int turnOrder, String questionText, String ttsAudioUrl) {
        Instant now = Instant.now();
        return ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId)
                .questionVersionId(questionVersionId)
                .turnType(TurnType.MAIN_QUESTION)
                .turnOrder(turnOrder)
                .questionText(questionText)
                .ttsAudioUrl(ttsAudioUrl)
                .status(TurnStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static ExamTurn createFollowUp(UUID attemptId, UUID questionId, UUID questionVersionId,
                                          UUID parentTurnId, int turnOrder, String questionText, String ttsAudioUrl) {
        Instant now = Instant.now();
        return ExamTurn.builder()
                .id(UUID.randomUUID())
                .attemptId(attemptId)
                .questionId(questionId)
                .questionVersionId(questionVersionId)
                .turnType(TurnType.FOLLOW_UP)
                .parentTurnId(parentTurnId)
                .turnOrder(turnOrder)
                .questionText(questionText)
                .ttsAudioUrl(ttsAudioUrl)
                .status(TurnStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void submitAnswer(String transcriptText, String studentAudioUrl) {
        if (this.status != TurnStatus.PENDING) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Turn is not pending an answer (current status: " + status + ")");
        }
        this.transcriptText = transcriptText;
        this.studentAudioUrl = studentAudioUrl;
        this.status = TurnStatus.ANSWERED;
        this.updatedAt = Instant.now();
    }

    public void evaluate(BigDecimal score, String feedback, String rubricEvaluations) {
        if (this.status != TurnStatus.ANSWERED) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Turn must be answered before evaluation (current status: " + status + ")");
        }
        this.score = score;
        this.feedback = feedback;
        this.rubricEvaluations = rubricEvaluations;
        this.status = TurnStatus.EVALUATED;
        this.updatedAt = Instant.now();
    }
}
