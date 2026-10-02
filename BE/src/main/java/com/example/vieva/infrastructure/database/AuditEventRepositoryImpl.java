package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.domain.entities.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public void saveAll(List<AuditEvent> events) {
        if (events == null || events.isEmpty()) return;
        List<AuditEventJpaEntity> entities = events.stream()
                .filter(event -> event != null)
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }
}
