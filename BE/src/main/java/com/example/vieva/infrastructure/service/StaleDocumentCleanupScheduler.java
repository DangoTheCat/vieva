package com.example.vieva.infrastructure.service;

import com.example.vieva.application.usecases.document.DocumentIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleDocumentCleanupScheduler {

    private final DocumentIndexingService documentIndexingService;

    @Scheduled(cron = "${vieva.schedule.stale-document-cleanup-cron:0 */10 * * * *}")
    public void cleanupStaleIndexingDocuments() {
        // Thresholds are scheduling concerns; the failure transition lives in the application service.
        Instant indexingThreshold = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant uploadedThreshold = Instant.now().minus(10, ChronoUnit.MINUTES);
        int failed = documentIndexingService.failStaleDocuments(indexingThreshold, uploadedThreshold);
        if (failed > 0) {
            log.info("Marked {} stale documents as FAILED", failed);
        }
    }
}
