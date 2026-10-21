package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.EmbeddingModelPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/**
 * Offline embeddings for dev/test ({@code vieva.ai.provider=mock}, the default). Feature hashing of
 * normalized words into a unit vector: texts sharing words get a higher cosine similarity, so
 * retrieval behaves sensibly without any API key.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vieva.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingGateway implements EmbeddingModelPort {

    private final int dimensions;

    public MockEmbeddingGateway(@Value("${vieva.rag.embedding-dimensions:1536}") int dimensions) {
        this.dimensions = dimensions;
        log.warn("Mock embedding provider active — set VIEVA_AI_PROVIDER=openai or gemini for real embeddings");
    }

    @Override
    public float[] generateEmbedding(String text) {
        float[] vector = new float[dimensions];
        Set<String> tokens = tokens(text);
        for (String token : tokens) {
            CRC32 crc = new CRC32();
            crc.update(token.getBytes(StandardCharsets.UTF_8));
            long hash = crc.getValue();
            int index = (int) (hash % dimensions);
            vector[index] += ((hash >> 20) & 1) == 0 ? 1f : -1f;
        }
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0) {
            // Empty text: a fixed unit vector keeps cosine distance defined.
            vector[0] = 1f;
            return vector;
        }
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
        return vector;
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        return texts == null ? List.of() : texts.stream().map(this::generateEmbedding).toList();
    }

    private static Set<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
