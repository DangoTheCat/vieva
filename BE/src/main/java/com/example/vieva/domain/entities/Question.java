package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Bản ghi gốc câu hỏi; ARCHIVED để ngừng cấp cho bài thi mới nhưng giữ vĩnh viễn.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private UUID questionId;
    private UUID topicId;
    private String questionCode;
    private QuestionStatus status;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
