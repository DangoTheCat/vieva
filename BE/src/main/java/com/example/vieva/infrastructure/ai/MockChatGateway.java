package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiChatPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Offline chat for dev/test ({@code vieva.ai.chat.provider=mock}, the default): echoes the question
 * and reports how big the received system prompt was, so the chat flow runs without an API key.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vieva.ai.chat.provider", havingValue = "mock", matchIfMissing = true)
public class MockChatGateway implements AiChatPort {

    public MockChatGateway() {
        log.warn("Mock chat provider active — replies are synthetic; set VIEVA_AI_CHAT_PROVIDER=groq");
    }

    @Override
    public String chat(String systemPrompt, String userMessage, double temperature, int maxTokens) {
        return "[MOCK] AIVES BOT đã nhận câu hỏi: \"" + userMessage + "\". "
                + "System prompt (kèm snapshot) dài " + systemPrompt.length() + " ký tự. "
                + "Đặt VIEVA_AI_CHAT_PROVIDER=groq để nhận câu trả lời thật.";
    }
}
