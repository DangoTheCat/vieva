package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSearchCriteria {
    private UUID subjectId;
    private UUID topicId;
    private BloomLevel bloomLevel;
    private QuestionApprovalStatus approvalStatus;
    private QuestionStatus status;
    private String keyword;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
