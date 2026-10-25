package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.application.ports.output.RuleBasedGraderPort;
import com.example.vieva.application.ports.output.VivaEvaluationContext;
import com.example.vieva.application.ports.output.VivaEvaluationGateway;
import com.example.vieva.application.ports.output.VivaEvaluationResult;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.GradeLevel;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.entities.ScoringMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Algorithmic fallback using keyword matching, length heuristics, and coverage ratio
 * if Gemini API is unreachable or when rule-based evaluation is required.
 */
@Slf4j
@Component
public class RuleBasedGraderFallback implements RuleBasedGraderPort, VivaEvaluationGateway {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{Alnum}\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS_VI = Set.of(
            "là", "và", "của", "các", "những", "cho", "với", "được", "trong", "đã",
            "về", "có", "không", "một", "này", "khi", "để", "như", "theo", "thì",
            "tại", "từ", "hay", "hoặc", "nếu", "bởi", "vì", "ra", "lại", "đến"
    );

    @Override
    public VivaEvaluationResult evaluateTurn(ExamTurn turn, Rubric rubric) {
        return evaluateTurn(turn, rubric, VivaEvaluationContext.empty());
    }

    @Override
    public VivaEvaluationResult evaluateTurn(ExamTurn turn, Rubric rubric, VivaEvaluationContext context) {
        String answer = turn.getTranscriptText();
        if (answer == null || answer.trim().isEmpty()) {
            return new VivaEvaluationResult(
                    false,
                    true,
                    "Bạn chưa đưa ra câu trả lời hoặc câu trả lời quá ngắn. Bạn có thể giải thích chi tiết hơn về câu hỏi vừa rồi được không?",
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "Chưa ghi nhận câu trả lời hoặc câu trả lời quá ngắn từ sinh viên.",
                    "{}"
            );
        }

        String questionText = turn.getQuestionText() != null ? turn.getQuestionText() : "";
        Set<String> questionKeywords = extractKeywords(questionText);

        Set<String> rubricKeywords = new HashSet<>();
        if (rubric != null && rubric.getDescription() != null) {
            rubricKeywords.addAll(extractKeywords(rubric.getDescription()));
        }

        Set<String> expectedKeywords = new HashSet<>(questionKeywords);
        expectedKeywords.addAll(rubricKeywords);
        if (context != null && context.expectedAnswer() != null && !context.expectedAnswer().isBlank()) {
            expectedKeywords.addAll(extractKeywords(context.expectedAnswer()));
        }

        Set<String> answerKeywords = extractKeywords(answer);

        // Keyword overlap / coverage ratio
        double coverageRatio = 0.0;
        if (!expectedKeywords.isEmpty()) {
            long matched = expectedKeywords.stream().filter(answerKeywords::contains).count();
            coverageRatio = (double) matched / expectedKeywords.size();
        } else {
            coverageRatio = 0.5;
        }

        // Length heuristic: viva answers typically need 15+ substantive words
        int wordCount = countWords(answer);
        double lengthRatio = Math.min(1.0, (double) wordCount / 20.0);

        // Weighted score 0 to 10
        // Score = (coverageRatio * 0.6 + lengthRatio * 0.4) * 10
        double rawScore = (coverageRatio * 0.6 + lengthRatio * 0.4) * 10.0;
        if (!Double.isFinite(rawScore)) {
            rawScore = 0.0;
        }
        BigDecimal finalScore = BigDecimal.valueOf(Math.min(10.0, Math.max(0.0, rawScore)))
                .setScale(2, RoundingMode.HALF_UP);

        boolean isStandardMet = finalScore.compareTo(BigDecimal.valueOf(5.0)) >= 0;
        boolean shouldFollowUp = !isStandardMet;

        String followUpQuestion = null;
        if (shouldFollowUp) {
            followUpQuestion = generateFollowUpQuestion(questionText, expectedKeywords, answerKeywords);
        }

        String feedback = String.format(
                "Đánh giá dự phòng (Rule-Based): Điểm %s/10. Độ bao phủ từ khóa: %.0f%%, độ dài phản hồi: %d từ. %s",
                finalScore,
                coverageRatio * 100,
                wordCount,
                isStandardMet ? "Câu trả lời đạt yêu cầu cơ bản." : "Câu trả lời chưa đủ ý hoặc còn sơ sài."
        );

        return new VivaEvaluationResult(
                isStandardMet,
                shouldFollowUp,
                followUpQuestion,
                finalScore,
                feedback,
                "{}"
        );
    }

    @Override
    public AttemptGradingResult grade(ExamAttempt attempt, List<ExamTurn> turns, Rubric rubric) {
        return gradeAttempt(attempt, turns, rubric);
    }

    public AttemptGradingResult gradeAttempt(ExamAttempt attempt, List<ExamTurn> turns, Rubric rubric) {
        if (turns == null || turns.isEmpty()) {
            return new AttemptGradingResult(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    GradeLevel.POOR,
                    "Không có câu trả lời nào trong lượt thi.",
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

        String detailedFeedback = String.format(
                "Tổng kết bài thi vấn đáp (Hệ thống dự phòng): Điểm trung bình %.2f/10 qua %d lượt vấn đáp. Xếp loại: %s.",
                avgDouble, evaluatedCount, grade.name()
        );

        return new AttemptGradingResult(average, grade, detailedFeedback, ScoringMode.RULE_BASED_FALLBACK);
    }

    private String generateFollowUpQuestion(String originalQuestion, Set<String> expectedKeywords, Set<String> answerKeywords) {
        List<String> missingKeywords = expectedKeywords.stream()
                .filter(k -> !answerKeywords.contains(k))
                .limit(3)
                .toList();

        if (!missingKeywords.isEmpty()) {
            return String.format("Bạn có thể làm rõ thêm mối liên hệ hoặc vai trò của khái niệm \"%s\" trong câu hỏi trên không?",
                    String.join(", ", missingKeywords));
        }
        return "Bạn hãy giải thích chi tiết hơn và lấy ví dụ minh họa cụ thể cho câu trả lời vừa rồi.";
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String cleaned = NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ");
        String[] tokens = WHITESPACE.split(cleaned.trim());

        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .filter(s -> !STOP_WORDS_VI.contains(s))
                .collect(Collectors.toSet());
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] words = WHITESPACE.split(text.trim());
        return words.length;
    }
}
