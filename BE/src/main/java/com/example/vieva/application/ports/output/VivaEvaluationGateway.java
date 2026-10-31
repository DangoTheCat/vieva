package com.example.vieva.application.ports.output;

/** Evaluates one answer and decides whether a follow-up question is needed. */
public interface VivaEvaluationGateway {
    VivaEvaluationResult evaluateTurn(EvaluationTurn turn, RubricSummary rubric, VivaEvaluationContext context);
}
