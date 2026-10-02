package com.example.vieva.adapters.presenters;

import com.example.vieva.domain.entities.QuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDetailDto {
    private UUID questionId;
    private UUID topicId;
    private String questionCode;
    private QuestionStatus status;
    private boolean hasPendingDraft;
    private QuestionVersionDto activeVersion;
    private QuestionVersionDto draftVersion;
    private RubricDto rubric;
    private List<QuestionSourceDto> sources;
    private List<QuestionVersionDto> versionHistory;
    private Instant createdAt;
    private Instant updatedAt;
}
