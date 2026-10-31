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
    private BigDecimal teacherScore;
    private String teacherFeedback;
    private UUID teacherId;
    private Instant teacherGradedAt;
    /** When the question finished being read to the student; the answer timer starts here. */
    private Instant askedAt;
    /** True when the answer arrived after the per-answer deadline (plus grace). */
    private boolean timedOut;
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
                .askedAt(now)
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
                .askedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Restarts the answer timer once the question has been read aloud (TTS finished), so reading
     * time is not counted against the student. Only a pending turn can be marked.
     */
    public void markAsked(Instant now) {
        if (this.status != TurnStatus.PENDING) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Turn is not pending an answer (current status: " + status + ")");
        }
        this.askedAt = now;
        this.updatedAt = now;
    }

    /** Deadline for the answer: askedAt + limit (the submit grace is applied by the caller). */
    public Instant answerDeadline(int answerTimeLimitSeconds) {
        Instant start = this.askedAt != null ? this.askedAt : this.createdAt;
        return start != null ? start.plusSeconds(answerTimeLimitSeconds) : null;
    }

    /** Records the answer and flags it as late when it arrives after deadline + grace. */
    public void submitAnswer(String transcriptText, String studentAudioUrl, Instant now,
                             int answerTimeLimitSeconds, int graceSeconds) {
        Instant deadline = answerDeadline(answerTimeLimitSeconds);
        submitAnswer(transcriptText, studentAudioUrl);
        this.timedOut = deadline != null && now.isAfter(deadline.plusSeconds(graceSeconds));
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

    public void gradeByTeacher(BigDecimal teacherScore, String teacherFeedback, UUID teacherId) {
        if (teacherScore != null && (teacherScore.compareTo(BigDecimal.ZERO) < 0 || teacherScore.compareTo(BigDecimal.TEN) > 0)) {
            throw new IllegalArgumentException("Score must be between 0 and 10");
        }
        this.teacherScore = teacherScore;
        this.teacherFeedback = teacherFeedback;
        this.teacherId = teacherId;
        this.teacherGradedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public BigDecimal getEffectiveScore() {
        if (teacherScore != null) {
            return teacherScore;
        }
        return score != null ? score : BigDecimal.ZERO;
    }
}
