package com.example.vieva.infrastructure.database;

import java.util.UUID;

public interface ChunkSearchResultProjection {
    UUID getChunkId();
    UUID getDocumentId();
    String getDocumentName();
    Integer getChunkIndex();
    String getContent();
    Double getDistance();
}
