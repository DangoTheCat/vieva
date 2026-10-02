package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.AiQuestionGeneratorPort;
import com.example.vieva.application.ports.output.ChunkSearchResult;
import com.example.vieva.application.ports.output.GeneratedCriterionItem;
import com.example.vieva.application.ports.output.GeneratedQuestionItem;
import com.example.vieva.domain.entities.BloomLevel;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpenAiQuestionGeneratorGateway implements AiQuestionGeneratorPort {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public record AiCriterionDto(
            @JsonPropertyDescription("Criterion title e.g. Concept Accuracy") String criterionName,
            @JsonPropertyDescription("Points for this criterion") BigDecimal maxPoints,
            @JsonPropertyDescription("Rubric scoring levels e.g. 0: Wrong, 2: Partial, 4: Complete") String achievementDescriptors,
            @JsonPropertyDescription("Order index starting from 1") int orderIndex
    ) {}

    public record AiQuestionDto(
            @JsonPropertyDescription("Clear, spoken question text suitable for TTS") String questionContent,
            @JsonPropertyDescription("Authoritative reference answer") String referenceAnswer,
            @JsonPropertyDescription("Cognitive Bloom level") String bloomLevel,
            @JsonPropertyDescription("Exact verbatim quote from the provided context chunks") String citationQuote,
            @JsonPropertyDescription("1-based index of the context chunk this citation is from") int chunkIndexRef,
            @JsonPropertyDescription("Title of the rubric e.g. Khung chấm điểm chuẩn") String rubricName,
            @JsonPropertyDescription("Total points of the rubric (must equal sum of criteria maxPoints)") BigDecimal totalPoints,
            @JsonPropertyDescription("General rubric guidance") String rubricDescription,
            @JsonPropertyDescription("List of evaluation criteria") List<AiCriterionDto> criteria
    ) {}

    public record AiQuestionListDto(
            @JsonPropertyDescription("List of generated questions") List<AiQuestionDto> questions
    ) {}

    public OpenAiQuestionGeneratorGateway(@Autowired(required = false) ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public List<GeneratedQuestionItem> generateQuestions(
            String topicName,
            BloomLevel bloomLevel,
            int quantity,
            String customPrompt,
            List<ChunkSearchResult> contextChunks) {

        if (contextChunks == null || contextChunks.isEmpty()) {
            return Collections.emptyList();
        }

        if (chatModel == null) {
            log.warn("ChatModel is not configured. Generating fallback mock question for local test/dev.");
            return generateFallbackQuestions(topicName, bloomLevel, quantity, contextChunks);
        }

        try {
            BeanOutputConverter<AiQuestionListDto> converter = new BeanOutputConverter<>(AiQuestionListDto.class);
            String formatInstructions = converter.getFormat();

            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < contextChunks.size(); i++) {
                ChunkSearchResult chunk = contextChunks.get(i);
                contextBuilder.append("=== CHUNK #").append(i + 1).append(" (Doc: ").append(chunk.documentName()).append(") ===\n")
                        .append(chunk.content()).append("\n\n");
            }

            String userPrompt = """
                    You are a university exam creator designing questions for an oral exam.
                    Topic: %s
                    Target Cognitive Level: %s
                    Number of questions to generate: %d
                    Instructor Custom Guidance: %s

                    STRICT REQUIREMENTS:
                    1. Every question and answer must be strictly derived from the Context below. Do not hallucinate or use outside knowledge.
                    2. For each question, you MUST provide an exact verbatim quote from one of the context chunks in 'citationQuote', and specify its 1-based chunk index in 'chunkIndexRef'.
                    3. The question text will be spoken to the student via Text-to-Speech (TTS), so it must be clear and conversational.
                    4. Each question must include a detailed Rubric. The sum of criteria maxPoints MUST EQUAL totalPoints exactly.
                    5. Return the result strictly conforming to the JSON schema below.

                    --- BEGIN CONTEXT ---
                    %s
                    --- END CONTEXT ---

                    %s
                    """.formatted(
                    topicName,
                    bloomLevel.name(),
                    quantity,
                    customPrompt != null ? customPrompt : "None",
                    contextBuilder.toString(),
                    formatInstructions
            );

            Prompt prompt = new Prompt(userPrompt);
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            AiQuestionListDto result = converter.convert(response);
            if (result == null || result.questions() == null) {
                return Collections.emptyList();
            }

            List<GeneratedQuestionItem> items = new ArrayList<>();
            for (AiQuestionDto dto : result.questions()) {
                int refIdx = Math.max(1, Math.min(contextChunks.size(), dto.chunkIndexRef())) - 1;
                ChunkSearchResult sourceChunk = contextChunks.get(refIdx);

                List<GeneratedCriterionItem> criteria = dto.criteria() != null ? dto.criteria().stream()
                        .map(c -> new GeneratedCriterionItem(
                                c.criterionName(),
                                c.maxPoints(),
                                c.achievementDescriptors(),
                                c.orderIndex()))
                        .collect(Collectors.toList()) : Collections.emptyList();

                BigDecimal total = dto.totalPoints() != null ? dto.totalPoints() : BigDecimal.TEN;

                items.add(new GeneratedQuestionItem(
                        dto.questionContent(),
                        dto.referenceAnswer(),
                        bloomLevel,
                        dto.citationQuote(),
                        sourceChunk.chunkId(),
                        sourceChunk.documentName(),
                        sourceChunk.similarityScore(),
                        dto.rubricName() != null ? dto.rubricName() : "Rubric đánh giá câu hỏi",
                        total,
                        dto.rubricDescription(),
                        criteria
                ));
            }
            return items;

        } catch (Exception e) {
            log.error("Failed to generate questions via LLM", e);
            throw new RuntimeException("AI Question Generation failed: " + e.getMessage(), e);
        }
    }

    private List<GeneratedQuestionItem> generateFallbackQuestions(
            String topicName, BloomLevel bloomLevel, int quantity, List<ChunkSearchResult> contextChunks) {
        List<GeneratedQuestionItem> fallback = new ArrayList<>();
        ChunkSearchResult chunk = contextChunks.get(0);

        String sampleQuote = chunk.content().substring(0, Math.min(chunk.content().length(), 80));

        for (int i = 1; i <= quantity; i++) {
            List<GeneratedCriterionItem> criteria = List.of(
                    new GeneratedCriterionItem("Độ chính xác khái niệm", new BigDecimal("5.00"), "0: Sai, 2.5: Đúng một phần, 5: Đầy đủ", 1),
                    new GeneratedCriterionItem("Lập luận và ví dụ", new BigDecimal("5.00"), "0: Không giải thích, 2.5: Chưa rõ ràng, 5: Logic", 2)
            );

            fallback.add(new GeneratedQuestionItem(
                    "Câu hỏi " + i + " về chủ đề " + topicName + ": Trình bày các nội dung chính dựa trên tài liệu?",
                    "Đáp án chuẩn mực dựa theo đoạn trích dẫn tài liệu môn học.",
                    bloomLevel,
                    sampleQuote,
                    chunk.chunkId(),
                    chunk.documentName(),
                    chunk.similarityScore(),
                    "Khung Rubric chuẩn môn học",
                    new BigDecimal("10.00"),
                    "Đánh giá năng lực hiểu và vận dụng kiến thức",
                    criteria
            ));
        }
        return fallback;
    }
}
