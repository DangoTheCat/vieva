package com.example.vieva.application.usecases.health;

import com.example.vieva.application.ports.output.health.DatabaseReadinessPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadinessCheckUseCaseImplTest {

    @Mock
    private DatabaseReadinessPort databaseReadinessPort;

    @Test
    @DisplayName("isReady returns true when database is ready")
    void isReadyReturnsTrueWhenDatabaseReady() {
        when(databaseReadinessPort.isDatabaseReady()).thenReturn(true);
        ReadinessCheckUseCaseImpl useCase = new ReadinessCheckUseCaseImpl(databaseReadinessPort);

        assertTrue(useCase.isReady());
    }

    @Test
    @DisplayName("isReady returns false when database is not ready")
    void isReadyReturnsFalseWhenDatabaseNotReady() {
        when(databaseReadinessPort.isDatabaseReady()).thenReturn(false);
        ReadinessCheckUseCaseImpl useCase = new ReadinessCheckUseCaseImpl(databaseReadinessPort);

        assertFalse(useCase.isReady());
    }

    @Test
    @DisplayName("isReady returns false when databaseReadinessPort is null")
    void isReadyReturnsFalseWhenPortNull() {
        ReadinessCheckUseCaseImpl useCase = new ReadinessCheckUseCaseImpl(null);

        assertFalse(useCase.isReady());
    }
}
