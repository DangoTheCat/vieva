package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.output.AttemptGradingGateway;
import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.application.ports.output.EvaluationTurn;
import com.example.vieva.application.ports.output.ExamAttemptQuestionSnapshotRepository;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.RubricSummary;
import com.example.vieva.application.ports.output.RuleBasedGraderPort;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamAttemptQuestionSnapshot;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.GradeLevel;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.ScoringMode;
import com.example.vieva.domain.entities.TurnStatus;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
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
    private final TransactionRunnerPort transactionRunner;

    public AttemptGradingServiceImpl(
            AttemptGradingGateway attemptGradingGateway,
            RuleBasedGraderPort ruleBasedGraderPort,
            RubricRepository rubricRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamScoreRepository examScoreRepository,
            TransactionRunnerPort transactionRunner
    ) {
        this(attemptGradingGateway, ruleBasedGraderPort, rubricRepository, examAttemptRepository, examScoreRepository, null, transactionRunner);
    }

    public AttemptGradingServiceImpl(
            AttemptGradingGateway attemptGradingGateway,
            RuleBasedGraderPort ruleBasedGraderPort,
            RubricRepository rubricRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamScoreRepository examScoreRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            TransactionRunnerPort transactionRunner
    ) {
        this.attemptGradingGateway = attemptGradingGateway;
        this.ruleBasedGraderPort = ruleBasedGraderPort;
        this.rubricRepository = rubricRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examScoreRepository = examScoreRepository;
        this.snapshotRepository = snapshotRepository;
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    private record Step1Result(
            ExamScore existingScore,
            List<EvaluationTurn> evalTurns,
            RubricSummary rubricSummary,
            List<ExamAttemptQuestionSnapshot> snapshots
    ) {}

    @Override
    public ExamScore gradeAndCompleteAttempt(ExamAttempt attempt, List<ExamTurn> turns) {
        if (attempt == null) {
            return null;
        }

        try {
            // Step 1: Short Transaction via transactionRunner.inNewTransaction
            Step1Result step1 = transactionRunner.inNewTransaction(() -> {
                // Lock attempt
                ExamAttempt lockedAttempt = examAttemptRepository.findByIdForUpdate(attempt.getId())
                        .orElse(null);
                if (lockedAttempt == null) {
                    log.warn("Attempt {} not found in repository during grading claim", attempt.getId());
                    return new Step1Result(null, null, null, null);
                }

                // Idempotency check: examScoreRepository.findByAttemptId(attempt.getId())
                Optional<ExamScore> existing = examScoreRepository.findByAttemptId(attempt.getId());
                if (existing.isPresent()) {
                    ExamScore existingScore = existing.get();
                    if (lockedAttempt.getStatus() == AttemptStatus.FINALIZING) {
                        lockedAttempt.syncSuccess();
                        lockedAttempt.recordScore(existingScore.getTotalScore(), existingScore.getGrade());
                        examAttemptRepository.save(lockedAttempt);
                    }
                    return new Step1Result(existingScore, null, null, null);
                }

                if (lockedAttempt.getStatus() != AttemptStatus.FINALIZING) {
                    log.warn("Attempt {} is in status {} (expected FINALIZING); cannot grade",
                            lockedAttempt.getId(), lockedAttempt.getStatus());
                    return new Step1Result(null, null, null, null);
                }

                // Read rubric
                Rubric rubric = null;
                if (turns != null && !turns.isEmpty()) {
                    UUID questionVersionId = turns.get(0).getQuestionVersionId();
                    if (questionVersionId != null && rubricRepository != null) {
                        rubric = rubricRepository.findByQuestionVersionId(questionVersionId).orElse(null);
                    }
                }

                // Read snapshots
                List<ExamAttemptQuestionSnapshot> snapshots = snapshotRepository != null
                        ? snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(lockedAttempt.getId())
                        : List.of();

                // Prepare EvaluationTurn list and RubricSummary using AiGradingInputs
                List<EvaluationTurn> evalTurns = AiGradingInputs.toEvaluationTurns(turns);
                RubricSummary rubricSummary = AiGradingInputs.toRubricSummary(rubric);

                return new Step1Result(null, evalTurns, rubricSummary, snapshots);
            });

            if (step1 == null) {
                return null;
            }

            // Step 2: Outside any Transaction, NO lock held
            if (step1.existingScore() != null) {
                return step1.existingScore();
            }

            if (step1.evalTurns() == null) {
                return null;
            }

            AttemptGradingResult gradingResult = null;
            try {
                if (attemptGradingGateway != null) {
                    gradingResult = attemptGradingGateway.gradeAttempt(step1.evalTurns(), step1.rubricSummary());
                }
            } catch (Exception ex) {
                log.warn("AttemptGradingGateway failed for attempt {}, falling back to rule-based grading: {}",
                        attempt.getId(), ex.getMessage());
            }

            if (gradingResult == null) {
                if (ruleBasedGraderPort != null) {
                    // Contract: if configured ruleBasedGraderPort throws an unrecoverable exception,
                    // it propagates to outer catch to transition attempt to INTERRUPTED.
                    gradingResult = ruleBasedGraderPort.grade(step1.evalTurns(), step1.rubricSummary());
                }
                if (gradingResult == null) {
                    gradingResult = fallbackRuleBasedGrading(turns);
                }
            }

            // Deterministic main-group score aggregation if snapshots are present
            List<ExamAttemptQuestionSnapshot> snapshots = step1.snapshots();
            if (snapshots != null && !snapshots.isEmpty()) {
                BigDecimal sum = BigDecimal.ZERO;
                List<ExamTurn> safeTurns = turns != null ? turns : List.of();
                for (ExamAttemptQuestionSnapshot snapshot : snapshots) {
                    UUID qId = snapshot.getQuestionId();
                    List<ExamTurn> questionTurns = safeTurns.stream()
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

                String feedback = gradingResult != null && gradingResult.detailedFeedback() != null
                        ? gradingResult.detailedFeedback()
                        : "Hoàn thành bài thi vấn đáp.";
                ScoringMode mode = gradingResult != null
                        ? parseScoringMode(gradingResult.scoringMode())
                        : ScoringMode.RULE_BASED_FALLBACK;
                gradingResult = new AttemptGradingResult(totalScore, gradeLevel.name(), feedback, mode.name());
            }

            // Prepare values for Step 3
            BigDecimal finalTotalScore = gradingResult.totalScore();
            GradeLevel finalGrade = parseGradeLevel(gradingResult.gradeLevel(), finalTotalScore);
            String finalFeedback = gradingResult.detailedFeedback();
            ScoringMode finalMode = parseScoringMode(gradingResult.scoringMode());

            // Step 3: Short Transaction via transactionRunner.inNewTransaction
            return transactionRunner.inNewTransaction(() -> {
                // Re-lock attempt
                ExamAttempt lockedAttempt = examAttemptRepository.findByIdForUpdate(attempt.getId())
                        .orElse(null);
                if (lockedAttempt == null) {
                    log.warn("Attempt {} not found in repository during grading finalization", attempt.getId());
                    return null;
                }

                // Double-check idempotency under lock
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

                // Revalidate persisted attempt status BEFORE saving ExamScore
                if (lockedAttempt.getStatus() != AttemptStatus.FINALIZING) {
                    log.warn("Attempt {} status changed to {} during external grading (expected FINALIZING); skipping score persistence and state transition",
                            lockedAttempt.getId(), lockedAttempt.getStatus());
                    return null;
                }

                // Save ExamScore
                ExamScore examScore = ExamScore.create(
                        lockedAttempt.getId(),
                        finalTotalScore,
                        finalGrade,
                        finalFeedback,
                        finalMode
                );
                ExamScore savedScore = examScoreRepository.save(examScore);

                // Update lockedAttempt: transition FINALIZING -> COMPLETED
                lockedAttempt.syncSuccess();
                lockedAttempt.recordScore(savedScore.getTotalScore(), savedScore.getGrade());
                examAttemptRepository.save(lockedAttempt);

                return savedScore;
            });

        } catch (Exception ex) {
            log.error("Unrecoverable error during attempt grading for attempt {}: {}",
                    attempt != null ? attempt.getId() : null, ex.getMessage(), ex);
            if (attempt != null && attempt.getId() != null) {
                try {
                    transactionRunner.inNewTransaction(() -> {
                        ExamAttempt toInterrupt = null;
                        try {
                            toInterrupt = examAttemptRepository.findByIdForUpdate(attempt.getId()).orElse(null);
                        } catch (Exception ignored) {
                        }
                        if (toInterrupt == null) {
                            try {
                                toInterrupt = examAttemptRepository.findById(attempt.getId()).orElse(null);
                            } catch (Exception ignored) {
                            }
                        }
                        if (toInterrupt != null && toInterrupt.getStatus() == AttemptStatus.FINALIZING) {
                            toInterrupt.syncError();
                            examAttemptRepository.save(toInterrupt);
                        }
                        if (attempt != null && attempt != toInterrupt && attempt.getStatus() == AttemptStatus.FINALIZING) {
                            try {
                                attempt.syncError();
                            } catch (Exception ignored) {
                            }
                        }
                        return null;
                    });
                } catch (Exception syncErr) {
                    log.error("Failed to mark attempt as INTERRUPTED: {}", syncErr.getMessage(), syncErr);
                    try {
                        if (attempt.getStatus() == AttemptStatus.FINALIZING) {
                            attempt.syncError();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            return null;
        }
    }

    private GradeLevel parseGradeLevel(String gradeLevelStr, BigDecimal score) {
        if (gradeLevelStr != null && !gradeLevelStr.isBlank()) {
            try {
                return GradeLevel.valueOf(gradeLevelStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                log.warn("Unknown grade level '{}', falling back to score calculation", gradeLevelStr);
            }
        }
        return GradeLevel.fromScore(score);
    }

    private ScoringMode parseScoringMode(String scoringModeStr) {
        if (scoringModeStr != null && !scoringModeStr.isBlank()) {
            try {
                return ScoringMode.valueOf(scoringModeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                log.warn("Unknown scoring mode '{}', falling back to RULE_BASED_FALLBACK", scoringModeStr);
            }
        }
        return ScoringMode.RULE_BASED_FALLBACK;
    }

    private AttemptGradingResult fallbackRuleBasedGrading(List<ExamTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return new AttemptGradingResult(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    GradeLevel.POOR.name(),
                    "No turns answered in this exam attempt.",
                    ScoringMode.RULE_BASED_FALLBACK.name()
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

        return new AttemptGradingResult(average, grade.name(), feedback, ScoringMode.RULE_BASED_FALLBACK.name());
    }
}
