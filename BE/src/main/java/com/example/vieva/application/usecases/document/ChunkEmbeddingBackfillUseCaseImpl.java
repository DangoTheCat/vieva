package com.example.vieva.application.usecases.document;

import com.example.vieva.application.ports.input.document.ChunkEmbeddingBackfillUseCase;
import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.domain.entities.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Chunks inserted by SQL seed scripts have no embedding, so similarity search (question generation,
 * knowledge search, grading context) never finds them. This embeds them with the configured model.
 * Like indexing, the embedding calls run outside any database transaction.
 */
@Slf4j
@RequiredArgsConstructor
public class ChunkEmbeddingBackfillUseCaseImpl implements ChunkEmbeddingBackfillUseCase {

    private static final int BATCH_SIZE = 50;

    private final DocumentChunkRepository documentChunkRepository;

    @Override
    public int backfillMissingEmbeddings() {
        Set<UUID> attempted = new HashSet<>();
        int embedded = 0;
        while (true) {
            // A chunk that comes back after an attempt could not be stored; skip it instead of looping
            List<DocumentChunk> batch = documentChunkRepository.findReadyChunksWithoutEmbedding(BATCH_SIZE).stream()
                    .filter(chunk -> attempted.add(chunk.getChunkId()))
                    .toList();
            if (batch.isEmpty()) {
                return embedded;
            }
            documentChunkRepository.storeEmbeddings(batch);
            embedded += batch.size();
            log.info("Embedded {} chunks that had no embedding", batch.size());
        }
    }
}
