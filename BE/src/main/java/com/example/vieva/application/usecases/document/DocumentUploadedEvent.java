package com.example.vieva.application.usecases.document;

import java.util.UUID;

public record DocumentUploadedEvent(UUID documentId, byte[] fileBytes) {}
