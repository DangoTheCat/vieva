package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.Rubric;

public interface VivaEvaluationGateway {
    VivaEvaluationResult evaluateTurn(ExamTurn turn, Rubric rubric);

    default VivaEvaluationResult evaluateTurn(ExamTurn turn, Rubric rubric, VivaEvaluationContext context) {
        return evaluateTurn(turn, rubric);
    }
}
