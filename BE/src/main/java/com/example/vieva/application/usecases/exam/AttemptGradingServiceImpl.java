package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.output.AttemptGradingGateway;
import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.RuleBasedGraderPort;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.GradeLevel;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.ScoringMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class AttemptGradingServiceImpl implements AttemptGradingService {

    private final AttemptGradingGateway attemptGradingGateway;
    private final RuleBasedGraderPort ruleBasedGraderPort;
    private final RubricRepository rubricRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamScoreRepository examScoreRepository;

    @Override
    @UseCaseTransaction
    public ExamScore gradeAndCompleteAttempt(ExamAttempt attempt, List<ExamTurn> turns) {
        Rubric rubric = null;
        if (!turns.isEmpty()) {
            UUID questionVersionId = turns.get(0).getQuestionVersionId();
            if (questionVersionId != null) {
                rubric = rubricRepository.findByQuestionVersionId(questionVersionId).orElse(null);
            }
        }

        AttemptGradingResult gradingResult = null;
        try {
            if (attemptGradingGateway != null) {
                gradingResult = attemptGradingGateway.gradeAttempt(attempt, turns, rubric);
            }
        } catch (Exception ex) {
            log.warn("AttemptGradingGateway failed for attempt {}, falling back to rule-based grading: {}",
                    attempt.getId(), ex.getMessage());
        }

        if (gradingResult == null) {
            if (ruleBasedGraderPort != null) {
                gradingResult = ruleBasedGraderPort.grade(attempt, turns, rubric);
            } else {
                gradingResult = fallbackRuleBasedGrading(turns);
            }
        }

        ExamScore examScore = ExamScore.create(
                attempt.getId(),
                gradingResult.totalScore(),
                gradingResult.gradeLevel(),
                gradingResult.detailedFeedback(),
                gradingResult.scoringMode()
        );
        ExamScore savedScore = examScoreRepository.save(examScore);

        attempt.complete();
        attempt.recordScore(savedScore.getTotalScore(), savedScore.getGrade());
        examAttemptRepository.save(attempt);

        return savedScore;
    }

    private AttemptGradingResult fallbackRuleBasedGrading(List<ExamTurn> turns) {
        if (turns.isEmpty()) {
            return new AttemptGradingResult(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    GradeLevel.POOR,
                    "No turns answered in this exam attempt.",
                    ScoringMode.RULE_BASED_FALLBACK
            );
        }

        BigDecimal sum = BigDecimal.ZERO;
        int evaluatedCount = 0;
        for (ExamTurn turn : turns) {
            if (turn.getScore() != null) {
                sum = sum.add(turn.getScore());
                evaluatedCount++;
            }
        }

        BigDecimal average = evaluatedCount > 0
                ? sum.divide(BigDecimal.valueOf(evaluatedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        GradeLevel grade;
        double avgDouble = average.doubleValue();
        if (avgDouble >= 8.5) {
            grade = GradeLevel.EXCELLENT;
        } else if (avgDouble >= 7.0) {
            grade = GradeLevel.GOOD;
        } else if (avgDouble >= 5.0) {
            grade = GradeLevel.AVERAGE;
        } else {
            grade = GradeLevel.POOR;
        }

        String feedback = String.format("Calculated via rule-based fallback across %d turns with average score %.2f.",
                evaluatedCount, average.doubleValue());

        return new AttemptGradingResult(average, grade, feedback, ScoringMode.RULE_BASED_FALLBACK);
    }
}
