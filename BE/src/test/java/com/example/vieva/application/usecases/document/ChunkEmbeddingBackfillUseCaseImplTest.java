package com.example.vieva.application.usecases.document;

import com.example.vieva.application.ports.output.DocumentChunkRepository;
import com.example.vieva.domain.entities.DocumentChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkEmbeddingBackfillUseCaseImplTest {

    @Mock private DocumentChunkRepository chunkRepository;
    @InjectMocks private ChunkEmbeddingBackfillUseCaseImpl useCase;

    @Test
    @DisplayName("Embeds chunks without embedding until none are left")
    void embedsMissingChunks() {
        DocumentChunk first = chunk();
        DocumentChunk second = chunk();
        when(chunkRepository.findReadyChunksWithoutEmbedding(anyInt()))
                .thenReturn(List.of(first, second))
                .thenReturn(List.of());

        int embedded = useCase.backfillMissingEmbeddings();

        assertThat(embedded).isEqualTo(2);
        verify(chunkRepository).storeEmbeddings(List.of(first, second));
    }

    @Test
    @DisplayName("Does nothing when every chunk already has an embedding")
    void noMissingChunks() {
        when(chunkRepository.findReadyChunksWithoutEmbedding(anyInt())).thenReturn(List.of());

        assertThat(useCase.backfillMissingEmbeddings()).isZero();
        verify(chunkRepository, never()).storeEmbeddings(anyList());
    }

    @Test
    @DisplayName("Stops instead of looping when a stored chunk still has no embedding")
    void stopsOnChunkThatComesBack() {
        DocumentChunk stuck = chunk();
        when(chunkRepository.findReadyChunksWithoutEmbedding(anyInt())).thenReturn(List.of(stuck));

        assertThat(useCase.backfillMissingEmbeddings()).isEqualTo(1);
        verify(chunkRepository, times(1)).storeEmbeddings(anyList());
    }

    private static DocumentChunk chunk() {
        return DocumentChunk.builder().chunkId(UUID.randomUUID()).documentId(UUID.randomUUID())
                .chunkIndex(0).content("text").build();
    }
}
