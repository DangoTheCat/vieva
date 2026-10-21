package com.example.vieva.application.ports.input.generation;

import com.example.vieva.application.ports.input.question.QuestionVersionView;
import lombok.Builder;

import java.util.List;

/**
 * Outcome of a generation run: request status/report and the drafts saved by this run.
 */
@Builder
public record GenerationResult(GenerationRequestResult request, List<QuestionVersionView> drafts) {
}
