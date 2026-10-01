package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.AuditEvent;

public interface AuditEventRepository {
    void save(AuditEvent event);
}
