package com.example.vieva.infrastructure.service;

import com.example.vieva.application.usecases.ai.AiContextSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiContextSnapshotScheduler {

    private final AiContextSnapshotService snapshotService;

    @Scheduled(fixedRateString = "${vieva.ai.snapshot.refresh-ms:60000}")
    public void refreshSnapshot() {
        try {
            snapshotService.refresh();
        } catch (RuntimeException e) {
            // The previous snapshot stays in the cache until a refresh succeeds.
            log.warn("AI context snapshot refresh failed: {}", e.getClass().getSimpleName());
        }
    }
}
