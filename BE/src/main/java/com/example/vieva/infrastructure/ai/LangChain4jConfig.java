package com.example.vieva.infrastructure.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LangChain4jConfig {

    @Bean
    ChatModel chatModel(@Value("${OPENAI_BASE_URL:https://api.openai.com/v1}") String baseUrl,
                        @Value("${OPENAI_API_KEY:test-dummy-key}") String apiKey,
                        @Value("${OPENAI_CHAT_MODEL:gpt-4o-mini}") String model,
                        @Value("${OPENAI_TEMPERATURE:0.4}") double temperature) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .strictJsonSchema(true)
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel(@Value("${OPENAI_BASE_URL:https://api.openai.com/v1}") String baseUrl,
                                  @Value("${OPENAI_API_KEY:test-dummy-key}") String apiKey,
                                  @Value("${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}") String model,
                                  @Value("${vieva.rag.embedding-dimensions:1536}") int dimensions) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .dimensions(dimensions)
                .build();
    }
}
