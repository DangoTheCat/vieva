package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.usecases.document.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisherGateway implements DomainEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishDocumentUploaded(UUID documentId, byte[] fileBytes) {
        eventPublisher.publishEvent(new DocumentUploadedEvent(documentId, fileBytes));
    }
}
