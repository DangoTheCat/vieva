package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.settings.RagSettings;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class PgVectorAdapterTest {
    @Test
    void batchesEmbeddingAndRejectsInvalidDimensions() {
        var model=mock(EmbeddingModelPort.class);
        var repo=mock(DocumentChunkJpaRepository.class);
        var settings=new RagSettings(); settings.setEmbeddingBatchSize(2); settings.setEmbeddingDimensions(4);
        var adapter=new PgVectorAdapter(repo,new DocumentChunkPersistenceMapper(),model,settings);
        when(model.generateEmbeddings(anyList())).thenAnswer(inv -> ((List<?>)inv.getArgument(0)).stream()
                .map(t -> new float[4]).toList());
        assertThat(adapter.embedChunks(List.of("a","b","c"))).hasSize(3);
        verify(model,times(2)).generateEmbeddings(anyList());
        when(model.generateEmbeddings(anyList())).thenReturn(List.of(new float[3]));
        assertThatThrownBy(() -> adapter.embedChunks(List.of("x"))).isInstanceOf(AiServiceException.class);
        verify(repo,never()).saveAll(any());
    }
}
