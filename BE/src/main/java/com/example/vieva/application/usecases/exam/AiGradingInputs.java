package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.output.EvaluationTurn;
import com.example.vieva.application.ports.output.RubricSummary;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.Rubric;

import java.util.List;

/** Maps domain entities to the plain inputs the AI grading gateways accept. */
final class AiGradingInputs {

    private AiGradingInputs() {
    }

    static EvaluationTurn toEvaluationTurn(ExamTurn turn) {
        return new EvaluationTurn(
                turn.getQuestionText(),
                turn.getTranscriptText(),
                turn.getTurnType() != null ? turn.getTurnType().name() : null,
                turn.getScore(),
                turn.getFeedback()
        );
    }

    static List<EvaluationTurn> toEvaluationTurns(List<ExamTurn> turns) {
        return turns == null ? List.of() : turns.stream().map(AiGradingInputs::toEvaluationTurn).toList();
    }

    static RubricSummary toRubricSummary(Rubric rubric) {
        return rubric == null ? null : new RubricSummary(rubric.getRubricName(), rubric.getDescription());
    }
}
