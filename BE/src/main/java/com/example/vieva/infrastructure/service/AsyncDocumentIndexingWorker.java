package com.example.vieva.infrastructure.service;

import com.example.vieva.application.usecases.document.DocumentIndexingService;
import com.example.vieva.application.usecases.document.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncDocumentIndexingWorker {

    private final DocumentIndexingService documentIndexingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        log.info("Triggering async indexing for document {}", event.documentId());
        try {
            documentIndexingService.indexDocument(event.documentId(), event.fileBytes());
        } catch (Exception e) {
            log.error("Async document indexing failed for {}", event.documentId(), e);
        }
    }
}
