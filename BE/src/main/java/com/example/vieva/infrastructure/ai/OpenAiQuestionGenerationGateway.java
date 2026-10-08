package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.QuestionGenerationPort;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM question generation through Spring AI ({@code vieva.ai.provider=openai}). Model, temperature
 * and API key come from {@code spring.ai.openai.*}; nothing sensitive is logged.
 */
@Slf4j
@Service
@ConditionalOnExpression("'openai'.equals('${vieva.ai.provider:mock}') || 'gemini'.equals('${vieva.ai.provider:mock}')")
public class OpenAiQuestionGenerationGateway implements QuestionGenerationPort {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public OpenAiQuestionGenerationGateway(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<GeneratedQuestionCandidate> generate(QuestionGenerationPrompt prompt) {
        String raw;
        try {
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(QuestionPromptBuilder.systemMessage()),
                    new UserMessage(QuestionPromptBuilder.userMessage(prompt)))));
            raw = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? null : response.getResult().getOutput().getText();
        } catch (RuntimeException e) {
            log.warn("LLM call failed: {}", e.getClass().getSimpleName());
            throw new AiServiceException("LLM provider call failed", e);
        }
        if (raw == null || raw.isBlank()) {
            throw new AiServiceException("LLM returned an empty response");
        }
        try {
            return objectMapper.readValue(extractJson(raw), LlmQuestionPayload.class).toCandidates();
        } catch (JsonProcessingException e) {
            log.warn("LLM response is not valid JSON for the question schema ({} chars)", raw.length());
            throw new AiServiceException("LLM response does not follow the JSON schema");
        }
    }

    /** Tolerates markdown fences or prose around the JSON object. */
    static String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return raw;
        }
        return raw.substring(start, end + 1);
    }
}
