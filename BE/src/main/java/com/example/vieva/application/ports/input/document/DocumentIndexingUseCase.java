package com.example.vieva.application.ports.input.document;

import java.util.UUID;

public interface DocumentIndexingUseCase {
    void indexDocument(UUID documentId);
}
