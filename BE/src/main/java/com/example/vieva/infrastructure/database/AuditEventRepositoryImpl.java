package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.domain.entities.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditEventRepositoryImpl implements AuditEventRepository {

    private final AuditEventJpaRepository jpaRepository;
    private final AuditEventPersistenceMapper mapper;

    @Override
    public void save(AuditEvent event) {
        if (event == null) return;
        jpaRepository.save(mapper.toEntity(event));
    }
}
