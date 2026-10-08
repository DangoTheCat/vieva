package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.usecases.document.DocumentUploadedEvent;
import com.example.vieva.application.usecases.user.AccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisherGateway implements DomainEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishDocumentIndexingRequested(UUID documentId) {
        eventPublisher.publishEvent(new DocumentUploadedEvent(documentId));
    }

    @Override
    public void publishAccountCreated(String email, String fullName, String roleCode, String temporaryPassword) {
        eventPublisher.publishEvent(new AccountCreatedEvent(email, fullName, roleCode, temporaryPassword));
    }
}
