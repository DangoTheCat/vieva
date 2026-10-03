package com.example.vieva.infrastructure.service;

import com.example.vieva.application.usecases.document.DocumentIndexingService;
import com.example.vieva.application.usecases.document.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Starts indexing only after the upload/retry transaction committed, so the worker always sees
 * the UPLOADED row; runs on the application task executor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncDocumentIndexingWorker {

    private final DocumentIndexingService documentIndexingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        log.info("Starting async indexing for document {}", event.documentId());
        try {
            documentIndexingService.indexDocument(event.documentId());
        } catch (Exception e) {
            log.error("Async document indexing crashed for {}", event.documentId(), e);
        }
    }
}
