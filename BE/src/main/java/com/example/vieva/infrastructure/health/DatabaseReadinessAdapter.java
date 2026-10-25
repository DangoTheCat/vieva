package com.example.vieva.infrastructure.health;

import com.example.vieva.application.ports.output.health.DatabaseReadinessPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseReadinessAdapter implements DatabaseReadinessPort {

    @Autowired(required = false)
    private DataSource dataSource;

    public DatabaseReadinessAdapter() {
    }

    public DatabaseReadinessAdapter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean isDatabaseReady() {
        if (dataSource == null) {
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
