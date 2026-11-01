package com.example.vieva.application.ports.input.document;

public interface ChunkEmbeddingBackfillUseCase {
    /** Embeds every chunk of a READY document that has no embedding yet; returns how many were embedded. */
    int backfillMissingEmbeddings();
}
