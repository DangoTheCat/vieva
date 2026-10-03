package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionVersion;
import lombok.Builder;

import java.util.List;

/**
 * UC1.4 detail: the version in force, the pending draft (if any) and the full history.
 *
 * @param currentVersion approved version in force, {@code null} when never approved
 * @param pendingDraft   DRAFT version awaiting review, {@code null} when none
 * @param history        all versions, newest first
 */
@Builder
public record QuestionDetailView(
        Question question,
        QuestionVersionView currentVersion,
        QuestionVersion pendingDraft,
        List<QuestionVersion> history
) {
}
