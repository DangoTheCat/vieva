package com.example.vieva.application.ports.output;

import java.util.List;

/**
 * LLM boundary for UC1.2. Implementations return raw candidates following the fixed JSON schema;
 * the application validates them (schema, Bloom, sources, rubric totals, duplicates).
 * Implementations throw {@link AiServiceException} when the provider fails or returns unparseable output.
 */
public interface QuestionGenerationPort {
    List<GeneratedQuestionCandidate> generate(QuestionGenerationPrompt prompt);
}
