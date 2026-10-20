package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.QuestionGenerationPort;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic offline generator for dev/test ({@code vieva.ai.provider=mock}, the default).
 * Builds schema-valid questions from sentences of the context chunks, with verbatim citations and
 * rubric totals that add up, so the full WF01 flow runs without an API key.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vieva.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockQuestionGenerationGateway implements QuestionGenerationPort {

    private static final Map<String, String> TEMPLATES = Map.of(
            "REMEMBER", "Hãy nêu lại nội dung chính của ý sau trong tài liệu: \"%s\"?",
            "UNDERSTAND", "Hãy giải thích bằng lời của bạn ý nghĩa của nhận định: \"%s\"?",
            "APPLY", "Hãy áp dụng kiến thức sau vào một tình huống thực tế cụ thể: \"%s\"?",
            "ANALYZE", "Hãy phân tích các thành phần và mối liên hệ trong nội dung: \"%s\"?",
            "EVALUATE", "Hãy đánh giá ưu điểm và hạn chế của quan điểm: \"%s\"?",
            "CREATE", "Hãy đề xuất một giải pháp hoặc thiết kế mới dựa trên ý: \"%s\"?");

    private final double duplicateThreshold;

    public MockQuestionGenerationGateway(
            @org.springframework.beans.factory.annotation.Value("${vieva.rag.duplicate-threshold:0.8}") double duplicateThreshold) {
        this.duplicateThreshold = duplicateThreshold;
        log.warn("Mock LLM provider active — generated questions are synthetic; set VIEVA_AI_PROVIDER=openai or gemini");
    }

    @Override
    public List<GeneratedQuestionCandidate> generate(QuestionGenerationPrompt prompt) {
        List<Sentence> sentences = sentences(prompt.contextChunks());
        if (sentences.isEmpty()) {
            return List.of();
        }
        List<Set<String>> used = new ArrayList<>();
        if (prompt.avoidQuestions() != null) {
            prompt.avoidQuestions().forEach(q -> used.add(tokens(q)));
        }
        if (prompt.previousContent() != null) {
            used.add(tokens(prompt.previousContent()));
        }

        List<GeneratedQuestionCandidate> result = new ArrayList<>();
        int cursor = 0;
        for (Map.Entry<String, Integer> entry : prompt.bloomDistribution().entrySet()) {
            for (int n = 0; n < entry.getValue(); n++) {
                GeneratedQuestionCandidate candidate = null;
                for (int tries = 0; tries < sentences.size() && candidate == null; tries++) {
                    Sentence sentence = sentences.get(cursor++ % sentences.size());
                    String template = TEMPLATES.getOrDefault(entry.getKey(), "Hãy giải thích nội dung: \"%s\"?");
                    String content = template.formatted(sentence.text());
                    if (isNearDuplicate(content, used, duplicateThreshold)) {
                        continue;
                    }
                    used.add(tokens(content));
                    candidate = build(entry.getKey(), content, sentence);
                }
                if (candidate != null) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    private GeneratedQuestionCandidate build(String level, String content, Sentence sentence) {
        return new GeneratedQuestionCandidate(
                content,
                "Theo tài liệu (" + sentence.ref() + "): " + sentence.text(),
                level,
                new BigDecimal("10"),
                List.of(
                        new GeneratedQuestionCandidate.Criterion("Độ chính xác nội dung",
                                "0: sai hoặc không trả lời; 3: đúng một phần; 6: đầy đủ và chính xác theo tài liệu",
                                new BigDecimal("6")),
                        new GeneratedQuestionCandidate.Criterion("Lập luận và diễn đạt",
                                "0: không có lập luận; 2: lập luận rời rạc; 4: mạch lạc, có ví dụ minh hoạ",
                                new BigDecimal("4"))),
                List.of(sentence.ref()),
                List.of(new GeneratedQuestionCandidate.Citation(sentence.ref(), sentence.text())));
    }

    private record Sentence(String ref, String text) {
    }

    private static List<Sentence> sentences(List<QuestionGenerationPrompt.ContextChunk> chunks) {
        List<Sentence> sentences = new ArrayList<>();
        for (QuestionGenerationPrompt.ContextChunk chunk : chunks) {
            for (String raw : chunk.content().split("(?<=[.!?])\\s+|\\n+")) {
                String text = raw.trim();
                if (text.length() >= 20 && text.length() <= 300) {
                    sentences.add(new Sentence(chunk.ref(), text));
                }
            }
        }
        return sentences;
    }

    private static Set<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        int union = a.size() + b.size() - intersection.size();
        return (double) intersection.size() / union;
    }

    private static boolean isNearDuplicate(String candidate, Collection<Set<String>> existingTokenSets, double threshold) {
        Set<String> candidateTokens = tokens(candidate);
        for (Set<String> existing : existingTokenSets) {
            if (jaccard(candidateTokens, existing) >= threshold) {
                return true;
            }
        }
        return false;
    }
}
