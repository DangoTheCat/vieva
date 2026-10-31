package com.example.vieva.application.ports.output;

import java.util.List;

/** Grades a whole attempt from its turns. */
public interface AttemptGradingGateway {
    AttemptGradingResult gradeAttempt(List<EvaluationTurn> turns, RubricSummary rubric);
}
