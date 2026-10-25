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
    private Instant createdAt;
    private Instant updatedAt;

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
