package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.AuditEvent;

import java.util.List;

public interface AuditEventRepository {
    void save(AuditEvent event);
    void saveAll(List<AuditEvent> events);
}
