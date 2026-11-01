package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.ports.input.document.ChunkEmbeddingBackfillUseCase;
import com.example.vieva.application.ports.input.user.EnsureDefaultRolesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final EnsureDefaultRolesUseCase ensureDefaultRolesUseCase;
    private final ChunkEmbeddingBackfillUseCase chunkEmbeddingBackfillUseCase;

    @Bean
    public CommandLineRunner initRoles() {
        return args -> ensureDefaultRolesUseCase.ensureDefaultRoles();
    }

    @Bean
    @ConditionalOnProperty(name = "vieva.rag.backfill-missing-embeddings", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner backfillChunkEmbeddings() {
        return args -> {
            // An unreachable embedding provider must not stop the app; the next start retries
            try {
                chunkEmbeddingBackfillUseCase.backfillMissingEmbeddings();
            } catch (RuntimeException e) {
                log.warn("Could not embed chunks that have no embedding: {}", e.getMessage());
            }
        };
    }
}
