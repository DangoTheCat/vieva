package com.example.vieva.application.ports.output;

import java.math.BigDecimal;

/**
 * What an AI grader needs to know about one interview turn. A plain data structure so gateways in
 * the infrastructure layer never receive domain entities (audit A2 / plan item 2.4).
 *
 * @param turnType MAIN_QUESTION or FOLLOW_UP
 */
public record EvaluationTurn(
        String questionText,
        String transcriptText,
        String turnType,
        BigDecimal score,
        String feedback
) {
}
