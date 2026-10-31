package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.application.ports.output.RuleBasedGraderPort;
import com.example.vieva.application.ports.output.EvaluationTurn;
import com.example.vieva.application.ports.output.RubricSummary;
import com.example.vieva.application.ports.output.VivaEvaluationContext;
import com.example.vieva.application.ports.output.VivaEvaluationGateway;
import com.example.vieva.application.ports.output.VivaEvaluationResult;
import com.example.vieva.domain.entities.GradeLevel;
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
    private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{L}\\p{N}\\s]");

    private static final Set<String> STOP_WORDS_VI = Set.of(
            "là", "và", "của", "các", "những", "cho", "với", "được", "trong", "đã",
            "về", "có", "không", "một", "này", "khi", "để", "như", "theo", "thì",
            "tại", "từ", "hay", "hoặc", "nếu", "bởi", "vì", "ra", "lại", "đến",
            "hãy", "nào", "gì", "sao", "rõ", "chỉ", "giữa"
    );
    // Keywords are compared without diacritics, so the stop words must be too
    private static final Set<String> STOP_WORDS = STOP_WORDS_VI.stream()
            .map(RuleBasedGraderFallback::normalize)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public VivaEvaluationResult evaluateTurn(EvaluationTurn turn, RubricSummary rubric, VivaEvaluationContext context) {
        String answer = turn.transcriptText();
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

        String questionText = turn.questionText() != null ? turn.questionText() : "";
        Set<String> questionKeywords = extractKeywords(questionText);

        Set<String> rubricKeywords = new HashSet<>();
        if (rubric != null && rubric.description() != null) {
            rubricKeywords.addAll(extractKeywords(rubric.description()));
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
    public AttemptGradingResult grade(List<EvaluationTurn> turns, RubricSummary rubric) {
        return gradeAttempt(turns, rubric);
    }

    public AttemptGradingResult gradeAttempt(List<EvaluationTurn> turns, RubricSummary rubric) {
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
        for (EvaluationTurn turn : turns) {
            if (turn.score() != null) {
                sum = sum.add(turn.score());
                evaluatedCount++;
            }
        }

        BigDecimal average = evaluatedCount > 0
                ? sum.divide(BigDecimal.valueOf(evaluatedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        GradeLevel grade = GradeLevel.fromScore(average);
        double avgDouble = average.doubleValue();

        String detailedFeedback = String.format(
                "Tổng kết bài thi vấn đáp (Hệ thống dự phòng): Điểm trung bình %.2f/10 trên %d/%d lượt vấn đáp đã trả lời. Xếp loại: %s.",
                avgDouble, evaluatedCount, turns.size(), gradeLabelVi(grade)
        );

        return new AttemptGradingResult(average, grade, detailedFeedback, ScoringMode.RULE_BASED_FALLBACK);
    }

    private String generateFollowUpQuestion(String originalQuestion, Set<String> expectedKeywords, Set<String> answerKeywords) {
        // Show the question's own (accented) words, in reading order, that the answer did not cover
        List<String> missingKeywords = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String questionText = originalQuestion != null ? originalQuestion : "";
        for (String word : WHITESPACE.split(PUNCTUATION.matcher(questionText).replaceAll(" ").trim())) {
            String key = normalize(word);
            if (word.length() >= 4 && expectedKeywords.contains(key) && !answerKeywords.contains(key) && seen.add(key)) {
                missingKeywords.add(word);
                if (missingKeywords.size() == 3) {
                    break;
                }
            }
        }

        if (!missingKeywords.isEmpty()) {
            return String.format("Bạn có thể làm rõ thêm mối liên hệ hoặc vai trò của khái niệm \"%s\" trong câu hỏi trên không?",
                    String.join(", ", missingKeywords));
        }
        return "Bạn hãy giải thích chi tiết hơn và lấy ví dụ minh họa cụ thể cho câu trả lời vừa rồi.";
    }

    // Same wording as the FE grade labels (utils/activeExam.js)
    private static String gradeLabelVi(GradeLevel grade) {
        return switch (grade) {
            case EXCELLENT -> "Xuất sắc";
            case GOOD -> "Khá";
            case AVERAGE -> "Trung bình";
            case POOR -> "Chưa đạt";
        };
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        String cleaned = NON_ALPHANUMERIC.matcher(normalize(text)).replaceAll(" ");
        String[] tokens = WHITESPACE.split(cleaned.trim());

        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .filter(s -> !STOP_WORDS.contains(s))
                .collect(Collectors.toSet());
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] words = WHITESPACE.split(text.trim());
        return words.length;
    }
}
