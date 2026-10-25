package com.example.vieva.application.usecases.health;

import com.example.vieva.application.ports.input.health.ReadinessCheckUseCase;
import com.example.vieva.application.ports.output.health.DatabaseReadinessPort;

public class ReadinessCheckUseCaseImpl implements ReadinessCheckUseCase {

    private final DatabaseReadinessPort databaseReadinessPort;

    public ReadinessCheckUseCaseImpl(DatabaseReadinessPort databaseReadinessPort) {
        this.databaseReadinessPort = databaseReadinessPort;
    }

    @Override
    public boolean isReady() {
        if (databaseReadinessPort == null) {
            return false;
        }
        return databaseReadinessPort.isDatabaseReady();
    }
}
