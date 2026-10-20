package com.example.vieva.application.ports.input.document;

import java.time.Instant;

public interface DocumentCleanupUseCase {
    int failStaleDocuments(Instant indexingThreshold, Instant uploadedThreshold);
}
