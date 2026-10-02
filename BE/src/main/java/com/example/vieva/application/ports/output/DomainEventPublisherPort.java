package com.example.vieva.application.ports.output;

import java.util.UUID;

/**
 * Output port for publishing domain events.
 * Keeps the Spring ApplicationEventPublisher out of application logic.
 */
public interface DomainEventPublisherPort {
    void publishDocumentUploaded(UUID documentId, byte[] fileBytes);
}
