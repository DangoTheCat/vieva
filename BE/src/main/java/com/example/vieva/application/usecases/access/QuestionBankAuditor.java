package com.example.vieva.application.usecases.access;

import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.domain.entities.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * BR-09: audit trail for group 1 (upload, indexing outcome, generation, edit, approve, reject,
 * archive, import). Written in the caller's transaction so the audit row commits with the change.
 */
@Component
@RequiredArgsConstructor
public class QuestionBankAuditor {

    public static final String DOCUMENT = "COURSE_DOCUMENT";
    public static final String QUESTION = "QUESTION";
    public static final String QUESTION_VERSION = "QUESTION_VERSION";
    public static final String GENERATION_REQUEST = "QUESTION_GENERATION_REQUEST";
    public static final String SUBJECT = "SUBJECT";

    private final AuditEventRepository auditEventRepository;
    private final JsonSerializerPort jsonSerializer;

    public void record(UUID actorId, String action, String entityType, UUID entityId,
                       Map<String, ?> oldValues, Map<String, ?> newValues) {
        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(actorId)
                .actionType(action)
                .entityType(entityType)
                .entityId(String.valueOf(entityId))
                .oldValuesJson(oldValues == null || oldValues.isEmpty() ? null : jsonSerializer.serialize(oldValues))
                .newValuesJson(newValues == null || newValues.isEmpty() ? null : jsonSerializer.serialize(newValues))
                .createdAt(Instant.now())
                .build());
    }

    public void record(UUID actorId, String action, String entityType, UUID entityId, Map<String, ?> newValues) {
        record(actorId, action, entityType, entityId, null, newValues);
    }
}
