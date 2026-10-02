package com.example.vieva.application.ports.output;

import java.math.BigDecimal;
import java.util.List;

/**
 * Raw LLM output item mirroring the fixed JSON schema:
 * content, expectedAnswer, bloomLevel, rubric (totalScore, criteria[name, description, maxScore]),
 * sourceChunkIds[], citations[chunkId, quote]. Any field may be missing or invalid.
 */
public record GeneratedQuestionCandidate(
        String content,
        String expectedAnswer,
        String bloomLevel,
        BigDecimal rubricTotalScore,
        List<Criterion> criteria,
        List<String> sourceChunkRefs,
        List<Citation> citations
) {
    public record Criterion(String name, String description, BigDecimal maxScore) {
    }

    public record Citation(String chunkRef, String quote) {
    }
}
