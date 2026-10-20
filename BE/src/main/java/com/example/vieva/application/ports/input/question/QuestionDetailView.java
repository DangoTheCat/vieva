package com.example.vieva.application.ports.input.question;

import lombok.Builder;

import java.util.List;

/**
 * UC1.4 detail: the version in force, the pending draft (if any) and the full history.
 *
 * @param question       owning question result
 * @param currentVersion approved version in force, {@code null} when never approved
 * @param pendingDraft   DRAFT version awaiting review, {@code null} when none
 * @param history        all versions, newest first
 */
@Builder
public record QuestionDetailView(
        QuestionResult question,
        QuestionVersionView currentVersion,
        QuestionVersionResult pendingDraft,
        List<QuestionVersionResult> history
) {
}
