package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.AuditEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AuditEventPersistenceMapper {

    public AuditEvent toDomain(AuditEventJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuditEvent.builder()
                .auditId(entity.getAuditId())
                .actorId(entity.getActorId())
                .actionType(entity.getActionType())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .oldValuesJson(entity.getOldValuesJson())
                .newValuesJson(entity.getNewValuesJson())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public AuditEventJpaEntity toEntity(AuditEvent domain) {
        if (domain == null) {
            return null;
        }
        return AuditEventJpaEntity.builder()
                .auditId(domain.getAuditId() != null ? domain.getAuditId() : UUID.randomUUID())
                .actorId(domain.getActorId())
                .actionType(domain.getActionType())
                .entityType(domain.getEntityType())
                .entityId(domain.getEntityId())
                .oldValuesJson(domain.getOldValuesJson())
                .newValuesJson(domain.getNewValuesJson())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now())
                .build();
    }
}
