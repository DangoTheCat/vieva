package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.EmbeddingModelPort;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnBean(EmbeddingModel.class)
public class LangChain4jEmbeddingGateway implements EmbeddingModelPort {

    private final EmbeddingModel embeddingModel;

    public LangChain4jEmbeddingGateway(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] generateEmbedding(String text) {
        try {
            return embeddingModel.embed(text).content().vector();
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
            return embeddingModel.embedAll(texts.stream()
                            .map(TextSegment::from)
                            .toList())
                    .content().stream()
                    .map(embedding -> embedding.vector())
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Batch embedding call failed for {} texts: {}", texts.size(), e.getClass().getSimpleName());
            throw new AiServiceException("Embedding provider call failed", e);
        }
    }
}
