package com.example.vieva.domain.entities;

public enum QuestionGenerationStatus {
    /** Request accepted, LLM not finished yet. */
    PENDING,
    /** Every requested question was generated and validated. */
    COMPLETED,
    /** Some questions were generated; the rest failed validation or the LLM gave up. */
    PARTIAL,
    /** No valid question was produced (LLM/embedding error or every candidate rejected). */
    FAILED
}
