package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Phiên bản câu hỏi; sửa câu đã APPROVED sẽ sinh DRAFT mới; bản cũ giữ tham chiếu lịch sử.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVersion {
    private UUID questionVersionId;
    private UUID questionId;
    private Integer versionNumber;
    private String questionContent;
    private String referenceAnswer;
    private BloomLevel bloomLevel;
    private QuestionGenerationMode generationMode;
    private QuestionApprovalStatus approvalStatus;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private String rejectionReason;
    private UUID createdBy;
    private Instant createdAt;
}
