package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.QuestionGenerationPort;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnBean(ChatModel.class)
public class LangChain4jQuestionGenerationGateway implements QuestionGenerationPort {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public LangChain4jQuestionGenerationGateway(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<GeneratedQuestionCandidate> generate(QuestionGenerationPrompt prompt) {
        String raw;
        try {
            var response = chatModel.chat(List.of(
                    SystemMessage.from(QuestionPromptBuilder.systemMessage()),
                    UserMessage.from(QuestionPromptBuilder.userMessage(prompt))));
            raw = response == null || response.aiMessage() == null ? null : response.aiMessage().text();
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

    static String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        return start >= 0 && end > start ? raw.substring(start, end + 1) : raw;
    }
}
