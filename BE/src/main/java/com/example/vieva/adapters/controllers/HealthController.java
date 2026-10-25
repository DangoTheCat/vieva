package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.health.ReadinessCheckUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Autowired(required = false)
    private ReadinessCheckUseCase readinessCheckUseCase;

    public HealthController() {
    }

    public HealthController(ReadinessCheckUseCase readinessCheckUseCase) {
        this.readinessCheckUseCase = readinessCheckUseCase;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "vieva-api"
        ));
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        boolean ready = readinessCheckUseCase != null && readinessCheckUseCase.isReady();
        if (ready) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "service", "vieva-api",
                    "database", "UP"
            ));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "DOWN",
                "service", "vieva-api",
                "database", "DOWN"
        ));
    }
}
