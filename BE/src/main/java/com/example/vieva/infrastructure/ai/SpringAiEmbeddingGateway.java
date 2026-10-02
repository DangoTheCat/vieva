package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Real embeddings through Spring AI ({@code vieva.ai.provider=openai}); model and API key come
 * from {@code spring.ai.openai.*} / environment variables.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vieva.ai.provider", havingValue = "openai")
public class SpringAiEmbeddingGateway implements EmbeddingModelPort {

    private final EmbeddingModel embeddingModel;

    public SpringAiEmbeddingGateway(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] generateEmbedding(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (RuntimeException e) {
            log.warn("Embedding call failed: {}", e.getClass().getSimpleName());
            throw new AiServiceException("Embedding provider call failed", e);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            return response.getResults().stream().map(Embedding::getOutput).toList();
        } catch (RuntimeException e) {
            log.warn("Batch embedding call failed for {} texts: {}", texts.size(), e.getClass().getSimpleName());
            throw new AiServiceException("Embedding provider call failed", e);
        }
    }
}
