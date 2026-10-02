package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.QuestionGenerationPort;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.services.QuestionSimilarity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic offline generator for dev/test ({@code vieva.ai.provider=mock}, the default).
 * Builds schema-valid questions from sentences of the context chunks, with verbatim citations and
 * rubric totals that add up, so the full WF01 flow runs without an API key.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vieva.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockQuestionGenerationGateway implements QuestionGenerationPort {

    private static final Map<BloomLevel, String> TEMPLATES = new EnumMap<>(Map.of(
            BloomLevel.REMEMBER, "Hãy nêu lại nội dung chính của ý sau trong tài liệu: \"%s\"?",
            BloomLevel.UNDERSTAND, "Hãy giải thích bằng lời của bạn ý nghĩa của nhận định: \"%s\"?",
            BloomLevel.APPLY, "Hãy áp dụng kiến thức sau vào một tình huống thực tế cụ thể: \"%s\"?",
            BloomLevel.ANALYZE, "Hãy phân tích các thành phần và mối liên hệ trong nội dung: \"%s\"?",
            BloomLevel.EVALUATE, "Hãy đánh giá ưu điểm và hạn chế của quan điểm: \"%s\"?",
            BloomLevel.CREATE, "Hãy đề xuất một giải pháp hoặc thiết kế mới dựa trên ý: \"%s\"?"));

    public MockQuestionGenerationGateway() {
        log.warn("Mock LLM provider active — generated questions are synthetic; set VIEVA_AI_PROVIDER=openai");
    }

    @Override
    public List<GeneratedQuestionCandidate> generate(QuestionGenerationPrompt prompt) {
        List<Sentence> sentences = sentences(prompt.contextChunks());
        if (sentences.isEmpty()) {
            return List.of();
        }
        List<Set<String>> used = new ArrayList<>();
        if (prompt.avoidQuestions() != null) {
            prompt.avoidQuestions().forEach(q -> used.add(QuestionSimilarity.tokens(q)));
        }
        if (prompt.previousContent() != null) {
            used.add(QuestionSimilarity.tokens(prompt.previousContent()));
        }

        List<GeneratedQuestionCandidate> result = new ArrayList<>();
        int cursor = 0;
        for (Map.Entry<BloomLevel, Integer> entry : prompt.bloomDistribution().entrySet()) {
            for (int n = 0; n < entry.getValue(); n++) {
                GeneratedQuestionCandidate candidate = null;
                for (int tries = 0; tries < sentences.size() && candidate == null; tries++) {
                    Sentence sentence = sentences.get(cursor++ % sentences.size());
                    String content = TEMPLATES.get(entry.getKey()).formatted(sentence.text());
                    if (QuestionSimilarity.isNearDuplicate(content, used, 0.8)) {
                        continue;
                    }
                    used.add(QuestionSimilarity.tokens(content));
                    candidate = build(entry.getKey(), content, sentence);
                }
                if (candidate != null) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    private GeneratedQuestionCandidate build(BloomLevel level, String content, Sentence sentence) {
        return new GeneratedQuestionCandidate(
                content,
                "Theo tài liệu (" + sentence.ref() + "): " + sentence.text(),
                level.name(),
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
}
