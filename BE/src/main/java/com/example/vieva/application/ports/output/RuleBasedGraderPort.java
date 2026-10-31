package com.example.vieva.application.ports.output;

import java.util.List;

/** Deterministic grader used when the LLM is unavailable. */
public interface RuleBasedGraderPort {
    AttemptGradingResult grade(List<EvaluationTurn> turns, RubricSummary rubric);
}
