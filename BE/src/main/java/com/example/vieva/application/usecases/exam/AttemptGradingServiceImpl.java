package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.output.AttemptGradingGateway;
import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.RuleBasedGraderPort;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.GradeLevel;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.ScoringMode;
import com.example.vieva.application.ports.output.ExamAttemptQuestionSnapshotRepository;
import com.example.vieva.domain.entities.ExamAttemptQuestionSnapshot;
import com.example.vieva.domain.entities.TurnStatus;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class AttemptGradingServiceImpl implements AttemptGradingService {

    private final AttemptGradingGateway attemptGradingGateway;
    private final RuleBasedGraderPort ruleBasedGraderPort;
    private final RubricRepository rubricRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamScoreRepository examScoreRepository;
    private final ExamAttemptQuestionSnapshotRepository snapshotRepository;

    public AttemptGradingServiceImpl(
            AttemptGradingGateway attemptGradingGateway,
            RuleBasedGraderPort ruleBasedGraderPort,
            RubricRepository rubricRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamScoreRepository examScoreRepository
    ) {
        this(attemptGradingGateway, ruleBasedGraderPort, rubricRepository, examAttemptRepository, examScoreRepository, null);
    }

    public AttemptGradingServiceImpl(
            AttemptGradingGateway attemptGradingGateway,
            RuleBasedGraderPort ruleBasedGraderPort,
            RubricRepository rubricRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamScoreRepository examScoreRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository
    ) {
        this.attemptGradingGateway = attemptGradingGateway;
        this.ruleBasedGraderPort = ruleBasedGraderPort;
        this.rubricRepository = rubricRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examScoreRepository = examScoreRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    @UseCaseTransaction
    public ExamScore gradeAndCompleteAttempt(ExamAttempt attempt, List<ExamTurn> turns) {
        if (attempt == null) {
            return null;
        }

        try {
            // Lock attempt for grading
            ExamAttempt lockedAttempt = examAttemptRepository.findByIdForUpdate(attempt.getId())
                    .orElse(attempt);

            // Idempotent score saving: check if already graded
            Optional<ExamScore> existing = examScoreRepository.findByAttemptId(attempt.getId());
            if (existing.isPresent()) {
                ExamScore existingScore = existing.get();
                if (lockedAttempt.getStatus() == AttemptStatus.FINALIZING) {
                    lockedAttempt.syncSuccess();
                    lockedAttempt.recordScore(existingScore.getTotalScore(), existingScore.getGrade());
                    examAttemptRepository.save(lockedAttempt);
                }
                return existingScore;
            }

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
                    gradingResult = attemptGradingGateway.gradeAttempt(
                            AiGradingInputs.toEvaluationTurns(turns), AiGradingInputs.toRubricSummary(rubric));
                }
            } catch (Exception ex) {
                log.warn("AttemptGradingGateway failed for attempt {}, falling back to rule-based grading: {}",
                        attempt.getId(), ex.getMessage());
            }

            if (gradingResult == null) {
                if (ruleBasedGraderPort != null) {
                    gradingResult = ruleBasedGraderPort.grade(
                            AiGradingInputs.toEvaluationTurns(turns), AiGradingInputs.toRubricSummary(rubric));
                } else {
                    gradingResult = fallbackRuleBasedGrading(turns);
                }
            }

            // P1 Deterministic main-group aggregation for snapshotted attempts:
            List<ExamAttemptQuestionSnapshot> snapshots = snapshotRepository != null
                    ? snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(lockedAttempt.getId())
                    : List.of();

            if (!snapshots.isEmpty()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (ExamAttemptQuestionSnapshot snapshot : snapshots) {
                    UUID qId = snapshot.getQuestionId();
                    List<ExamTurn> questionTurns = turns.stream()
                            .filter(t -> qId != null && qId.equals(t.getQuestionId()))
                            .toList();

                    BigDecimal questionScore = BigDecimal.ZERO;
                    Optional<ExamTurn> teacherGraded = questionTurns.stream()
                            .filter(t -> t.getTeacherScore() != null)
                            .findFirst();
                    if (teacherGraded.isPresent()) {
                        questionScore = teacherGraded.get().getTeacherScore();
                    } else {
                        Optional<ExamTurn> lastEvaluated = questionTurns.stream()
                                .filter(t -> t.getStatus() == TurnStatus.EVALUATED && t.getScore() != null)
                                .reduce((first, second) -> second);
                        if (lastEvaluated.isPresent()) {
                            questionScore = lastEvaluated.get().getScore();
                        }
                    }
                    sum = sum.add(questionScore);
                }

                BigDecimal totalScore = sum.divide(BigDecimal.valueOf(snapshots.size()), 2, RoundingMode.HALF_UP);
                GradeLevel gradeLevel = GradeLevel.fromScore(totalScore);

                String feedback = gradingResult != null ? gradingResult.detailedFeedback() : "Hoàn thành bài thi vấn đáp.";
                ScoringMode mode = gradingResult != null ? gradingResult.scoringMode() : ScoringMode.RULE_BASED_FALLBACK;
                gradingResult = new AttemptGradingResult(totalScore, gradeLevel, feedback, mode);
            }

            ExamScore examScore = ExamScore.create(
                    lockedAttempt.getId(),
                    gradingResult.totalScore(),
                    gradingResult.gradeLevel(),
                    gradingResult.detailedFeedback(),
                    gradingResult.scoringMode()
            );
            ExamScore savedScore = examScoreRepository.save(examScore);

            if (lockedAttempt.getStatus() == AttemptStatus.FINALIZING) {
                lockedAttempt.syncSuccess();
            }
            lockedAttempt.recordScore(savedScore.getTotalScore(), savedScore.getGrade());
            examAttemptRepository.save(lockedAttempt);

            return savedScore;
        } catch (Exception ex) {
            log.error("Unrecoverable error during attempt grading for attempt {}: {}", attempt.getId(), ex.getMessage(), ex);
            try {
                ExamAttempt toInterrupt = examAttemptRepository.findByIdForUpdate(attempt.getId())
                        .or(() -> examAttemptRepository.findById(attempt.getId()))
                        .orElse(attempt);
                if (toInterrupt.getStatus() == AttemptStatus.FINALIZING) {
                    toInterrupt.syncError();
                    examAttemptRepository.save(toInterrupt);
                }
            } catch (Exception syncErr) {
                log.error("Failed to mark attempt as INTERRUPTED: {}", syncErr.getMessage(), syncErr);
            }
            return null;
        }
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

        GradeLevel grade = GradeLevel.fromScore(average);

        String feedback = String.format("Calculated via rule-based fallback across %d turns with average score %.2f.",
                evaluatedCount, average.doubleValue());

        return new AttemptGradingResult(average, grade, feedback, ScoringMode.RULE_BASED_FALLBACK);
    }
}
