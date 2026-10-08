package com.example.vieva.application.ports.output;

import java.util.UUID;

/**
 * Output port for publishing domain events.
 * Keeps the Spring ApplicationEventPublisher out of application logic.
 */
public interface DomainEventPublisherPort {
    /** Fired inside the upload/retry transaction; indexing starts after commit. */
    void publishDocumentIndexingRequested(UUID documentId);

    /** Fired inside the admin create-user transaction; the welcome email is sent after commit. */
    void publishAccountCreated(String email, String fullName, String roleCode, String temporaryPassword);
}
