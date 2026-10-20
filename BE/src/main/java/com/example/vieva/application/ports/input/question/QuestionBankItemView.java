package com.example.vieva.application.ports.input.question;

import lombok.Builder;

/**
 * UC1.4 search row: the approved version in force plus whether a new draft is pending.
 */
@Builder
public record QuestionBankItemView(QuestionVersionView current, boolean hasPendingDraft) {
}
