package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.EmbeddingModelPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SpringAiEmbeddingGateway implements EmbeddingModelPort {

    private final EmbeddingModel embeddingModel;

    public SpringAiEmbeddingGateway(@Autowired(required = false) EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (embeddingModel == null) {
            log.warn("EmbeddingModel is not configured, returning 1536-dim zero vector");
            return new float[1536];
        }
        return embeddingModel.embed(text);
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        if (embeddingModel == null) {
            log.warn("EmbeddingModel is not configured, returning empty zero vectors");
            return texts.stream().map(t -> new float[1536]).collect(Collectors.toList());
        }
        EmbeddingResponse response = embeddingModel.embedForResponse(texts);
        return response.getResults().stream()
                .map(r -> r.getOutput())
                .collect(Collectors.toList());
    }
}
