package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Bản ghi gốc câu hỏi (định danh bền vững); ARCHIVED để ngừng cấp cho bài thi mới nhưng giữ vĩnh viễn.
 * Chỉ xuất hiện trong ngân hàng chính thức khi có {@code currentApprovedVersionId}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private UUID questionId;
    private UUID subjectId;
    private UUID topicId;
    private String questionCode;
    private QuestionStatus status;
    private UUID currentApprovedVersionId;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public static Question newDraftOwner(UUID subjectId, UUID topicId, UUID createdBy) {
        Instant now = Instant.now();
        return Question.builder()
                .questionId(UUID.randomUUID())
                .subjectId(subjectId)
                .topicId(topicId)
                .questionCode("Q-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .status(QuestionStatus.ACTIVE)
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isArchived() {
        return status == QuestionStatus.ARCHIVED;
    }

    public boolean isInBank() {
        return currentApprovedVersionId != null;
    }

    public void ensureActive() {
        if (isArchived()) {
            throw new AppException(ErrorCode.QUESTION_ARCHIVED);
        }
    }

    /** Points the bank at a newly approved version (UC1.3 is the only caller — BR-05). */
    public void publish(UUID approvedVersionId) {
        ensureActive();
        this.currentApprovedVersionId = approvedVersionId;
        this.updatedAt = Instant.now();
    }

    /** ACTIVE → ARCHIVED. Never deletes anything (BR-04). */
    public void archive() {
        ensureActive();
        this.status = QuestionStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }
}
