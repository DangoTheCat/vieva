package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.QuestionGenerationRequest;

import java.util.List;

/**
 * Outcome of a generation run: request status/report and the drafts saved by this run.
 */
public record GenerationResult(QuestionGenerationRequest request, List<QuestionVersionView> drafts) {
}
