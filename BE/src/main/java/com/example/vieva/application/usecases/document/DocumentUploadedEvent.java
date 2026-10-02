package com.example.vieva.application.usecases.document;

import java.util.UUID;

/**
 * Published inside the upload/retry transaction; the async worker indexes the document after commit.
 * Carries only the id: the bytes are re-read from storage, which also makes retries possible.
 */
public record DocumentUploadedEvent(UUID documentId) {}
