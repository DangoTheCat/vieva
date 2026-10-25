package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.ports.input.health.ReadinessCheckUseCase;
import com.example.vieva.application.ports.output.health.DatabaseReadinessPort;
import com.example.vieva.application.usecases.health.ReadinessCheckUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfig {
    @Bean
    public ReadinessCheckUseCase readinessCheckUseCase(DatabaseReadinessPort databaseReadinessPort) {
        return new ReadinessCheckUseCaseImpl(databaseReadinessPort);
    }
}
