package com.example.vieva.application.ports.output;

/**
 * UC1.4 search row: the approved version in force plus whether a new draft is pending.
 */
public record QuestionBankItemView(QuestionVersionView current, boolean hasPendingDraft) {
}
