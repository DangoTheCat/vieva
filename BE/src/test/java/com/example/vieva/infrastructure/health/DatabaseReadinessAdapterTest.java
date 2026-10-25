package com.example.vieva.infrastructure.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseReadinessAdapterTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Test
    @DisplayName("isDatabaseReady returns true when connection is valid within 2 seconds")
    void returnsTrueWhenConnectionValid() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        DatabaseReadinessAdapter adapter = new DatabaseReadinessAdapter(dataSource);
        assertTrue(adapter.isDatabaseReady());
    }

    @Test
    @DisplayName("isDatabaseReady returns false when connection is not valid")
    void returnsFalseWhenConnectionInvalid() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        DatabaseReadinessAdapter adapter = new DatabaseReadinessAdapter(dataSource);
        assertFalse(adapter.isDatabaseReady());
    }

    @Test
    @DisplayName("isDatabaseReady returns false when getConnection throws exception")
    void returnsFalseOnConnectionException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("DB connection failure"));

        DatabaseReadinessAdapter adapter = new DatabaseReadinessAdapter(dataSource);
        assertFalse(adapter.isDatabaseReady());
    }

    @Test
    @DisplayName("isDatabaseReady returns false when dataSource is null")
    void returnsFalseWhenDataSourceNull() {
        DatabaseReadinessAdapter adapter = new DatabaseReadinessAdapter(null);
        assertFalse(adapter.isDatabaseReady());
    }
}
