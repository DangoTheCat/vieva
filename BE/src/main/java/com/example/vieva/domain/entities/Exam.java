package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Exam {
    private UUID id;
    private UUID subjectId;
    private UUID creatorId;
    private String title;
    @Builder.Default
    private int durationSeconds = 900;
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;
    /** Seconds a student has to answer each question (main or follow-up). */
    @Builder.Default
    private int answerTimeLimitSeconds = DEFAULT_ANSWER_TIME_LIMIT_SECONDS;
    /** Maximum adaptive follow-up questions the AI may ask per main question. */
    @Builder.Default
    private int maxFollowUpsPerQuestion = DEFAULT_MAX_FOLLOW_UPS;
    private Instant createdAt;
    private Instant updatedAt;

    public static final int DEFAULT_ANSWER_TIME_LIMIT_SECONDS = 120;
    public static final int MIN_ANSWER_TIME_LIMIT_SECONDS = 30;
    public static final int MAX_ANSWER_TIME_LIMIT_SECONDS = 600;
    public static final int DEFAULT_MAX_FOLLOW_UPS = 2;
    public static final int MAX_FOLLOW_UPS_LIMIT = 3;

    public static Exam create(UUID subjectId, UUID creatorId, String title, Integer durationSeconds,
                              Integer answerTimeLimitSeconds, Integer maxFollowUpsPerQuestion) {
        Exam exam = create(subjectId, creatorId, title, durationSeconds);
        exam.applyInterviewSettings(answerTimeLimitSeconds, maxFollowUpsPerQuestion);
        return exam;
    }

    /**
     * Sets the per-answer time limit and the follow-up budget. Null keeps the current value.
     * Only a DRAFT exam can change them, so every attempt of an exam runs under the same rules.
     */
    public void applyInterviewSettings(Integer answerTimeLimitSeconds, Integer maxFollowUpsPerQuestion) {
        if (!isDraft()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Interview settings can only be changed while the exam is in DRAFT status");
        }
        if (answerTimeLimitSeconds != null) {
            if (answerTimeLimitSeconds < MIN_ANSWER_TIME_LIMIT_SECONDS || answerTimeLimitSeconds > MAX_ANSWER_TIME_LIMIT_SECONDS) {
                throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                        "Answer time limit must be between " + MIN_ANSWER_TIME_LIMIT_SECONDS + " and "
                                + MAX_ANSWER_TIME_LIMIT_SECONDS + " seconds");
            }
            this.answerTimeLimitSeconds = answerTimeLimitSeconds;
        }
        if (maxFollowUpsPerQuestion != null) {
            if (maxFollowUpsPerQuestion < 0 || maxFollowUpsPerQuestion > MAX_FOLLOW_UPS_LIMIT) {
                throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                        "Max follow-ups per question must be between 0 and " + MAX_FOLLOW_UPS_LIMIT);
            }
            this.maxFollowUpsPerQuestion = maxFollowUpsPerQuestion;
        }
        this.updatedAt = Instant.now();
    }

    public static Exam create(UUID subjectId, UUID creatorId, String title, Integer durationSeconds) {
        if (subjectId == null || creatorId == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST, "Subject ID and Creator ID are required");
        }
        if (title == null || title.isBlank()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST, "Exam title is required");
        }
        int duration = (durationSeconds != null && durationSeconds > 0) ? durationSeconds : 900;
        Instant now = Instant.now();
        return Exam.builder()
                .id(UUID.randomUUID())
                .subjectId(subjectId)
                .creatorId(creatorId)
                .title(title.trim())
                .durationSeconds(duration)
                .status(ExamStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isDraft() {
        return status == ExamStatus.DRAFT;
    }

    public boolean isInProgress() {
        return status == ExamStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == ExamStatus.COMPLETED;
    }

    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST, "Exam title cannot be blank");
        }
        this.title = newTitle.trim();
        this.updatedAt = Instant.now();
    }

    public void transitionTo(ExamStatus newStatus) {
        if (newStatus == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST, "Target status cannot be null");
        }
        if (this.status == newStatus) {
            return;
        }
        if (this.status == ExamStatus.DRAFT && newStatus == ExamStatus.IN_PROGRESS) {
            this.status = ExamStatus.IN_PROGRESS;
        } else if (this.status == ExamStatus.IN_PROGRESS && newStatus == ExamStatus.COMPLETED) {
            this.status = ExamStatus.COMPLETED;
        } else {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Invalid exam status transition from " + this.status + " to " + newStatus);
        }
        this.updatedAt = Instant.now();
    }

    public void ensureCanModifyQuestions() {
        if (!isDraft()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUEST,
                    "Questions can only be modified when exam is in DRAFT status");
        }
    }
}
