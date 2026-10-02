package com.example.vieva.adapters.presenters;

import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVersionDto {
    private UUID versionId;
    private UUID questionId;
    private Integer versionNumber;
    private String questionContent;
    private String referenceAnswer;
    private BloomLevel bloomLevel;
    private QuestionGenerationMode generationMode;
    private QuestionApprovalStatus approvalStatus;
    private String rejectionReason;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private Instant createdAt;
}
