package com.example.vieva.infrastructure.service;

import com.example.vieva.application.usecases.document.DocumentIndexingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class StaleDocumentCleanupScheduler {

    private final DocumentIndexingService documentIndexingService;
    private final Duration indexingTimeout;
    private final Duration uploadedTimeout;

    public StaleDocumentCleanupScheduler(
            DocumentIndexingService documentIndexingService,
            @Value("${vieva.documents.indexing-timeout:PT30M}") Duration indexingTimeout,
            @Value("${vieva.documents.uploaded-timeout:PT15M}") Duration uploadedTimeout) {
        this.documentIndexingService = documentIndexingService;
        this.indexingTimeout = indexingTimeout;
        this.uploadedTimeout = uploadedTimeout;
    }

    @Scheduled(cron = "${vieva.schedule.stale-document-cleanup-cron:0 */10 * * * *}")
    public void cleanupStaleIndexingDocuments() {
        // Thresholds are scheduling concerns; the failure transition lives in the application service.
        Instant now = Instant.now();
        int failed = documentIndexingService.failStaleDocuments(now.minus(indexingTimeout), now.minus(uploadedTimeout));
        if (failed > 0) {
            log.info("Marked {} stale documents as FAILED", failed);
        }
    }
}
