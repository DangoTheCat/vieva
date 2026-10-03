package com.example.vieva.application.ports.output;

/**
 * Single-turn chat with an LLM. Implementations return the visible reply only (no reasoning tags)
 * and throw {@link AiServiceException} when the provider fails.
 */
public interface AiChatPort {
    String chat(String systemPrompt, String userMessage, double temperature, int maxTokens);
}
