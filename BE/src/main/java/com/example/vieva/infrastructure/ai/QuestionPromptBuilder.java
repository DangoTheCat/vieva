package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.example.vieva.domain.entities.BloomLevel;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the system/user messages for question generation. The output contract is the fixed JSON
 * schema of {@link LlmQuestionPayload}; the application validates every item afterwards.
 */
public final class QuestionPromptBuilder {

    private QuestionPromptBuilder() {
    }

    public static String systemMessage() {
        return """
                You are an assessment designer writing ORAL (viva) exam questions for a university course.
                Rules:
                1. Use ONLY the provided context chunks. Never use outside knowledge.
                2. Every question must cite at least one chunk in "sourceChunkIds" using the chunk ids shown (e.g. "C2"),
                   and give at least one "citations" entry whose "quote" is copied VERBATIM from that chunk.
                3. "bloomLevel" must be exactly one of: REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE
                   (revised Bloom taxonomy: Nhớ, Hiểu, Vận dụng, Phân tích, Đánh giá, Sáng tạo).
                   Produce exactly the requested number of questions for each level.
                4. Each question has a rubric: 2-4 criteria with a name, a description of achievement levels and a
                   positive "maxScore". "totalScore" MUST equal the sum of the criteria maxScore (no weighting).
                5. Write questions and answers in the language of the context (Vietnamese if the context is Vietnamese).
                   Questions must be answerable orally in 2-5 minutes; do not repeat the questions to avoid.
                6. Reply with ONE JSON object only, no markdown, matching:
                {"questions":[{"content":"string","expectedAnswer":"string","bloomLevel":"APPLY",
                  "rubric":{"totalScore":10,"criteria":[{"name":"string","description":"string","maxScore":5}]},
                  "sourceChunkIds":["C1"],"citations":[{"chunkId":"C1","quote":"verbatim text from C1"}]}]}
                """;
    }

    public static String userMessage(QuestionGenerationPrompt prompt) {
        StringBuilder builder = new StringBuilder();
        builder.append("Subject: ").append(nullToDash(prompt.subjectName())).append('\n');
        builder.append("Topic: ").append(nullToDash(prompt.topicName())).append('\n');
        builder.append("Questions per Bloom level: ").append(describe(prompt.bloomDistribution())).append('\n');
        builder.append("Total questions: ").append(prompt.totalQuestions()).append('\n');
        if (prompt.lecturerNote() != null && !prompt.lecturerNote().isBlank()) {
            builder.append("Lecturer guidance: ").append(prompt.lecturerNote().trim()).append('\n');
        }
        if (prompt.previousContent() != null) {
            builder.append("\nREGENERATION: write ONE replacement for this rejected draft question:\n")
                    .append(prompt.previousContent()).append('\n');
            builder.append("Lecturer feedback to address: ")
                    .append(prompt.lecturerFeedback() == null || prompt.lecturerFeedback().isBlank()
                            ? "(none)" : prompt.lecturerFeedback().trim())
                    .append('\n');
        }
        if (prompt.avoidQuestions() != null && !prompt.avoidQuestions().isEmpty()) {
            builder.append("\nQuestions to avoid (already in the bank):\n");
            prompt.avoidQuestions().forEach(q -> builder.append("- ").append(q).append('\n'));
        }
        builder.append("\n--- CONTEXT ---\n");
        for (QuestionGenerationPrompt.ContextChunk chunk : prompt.contextChunks()) {
            builder.append("[").append(chunk.ref()).append("] (document: ").append(chunk.documentName());
            if (chunk.page() != null) {
                builder.append(", page ").append(chunk.page());
            }
            builder.append(")\n").append(chunk.content()).append("\n\n");
        }
        builder.append("--- END CONTEXT ---\n");
        return builder.toString();
    }

    private static String describe(Map<BloomLevel, Integer> distribution) {
        return distribution.entrySet().stream()
                .map(entry -> entry.getKey().name() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
