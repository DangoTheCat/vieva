package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Nhật ký kiểm toán bảo mật và hành vi quản trị.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private UUID auditId;
    private UUID actorId;
    private String actionType;
    private String entityType;
    private String entityId;
    private String oldValuesJson;
    private String newValuesJson;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}
