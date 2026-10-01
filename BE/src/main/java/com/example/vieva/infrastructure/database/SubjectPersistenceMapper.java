package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.Subject;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SubjectPersistenceMapper {

    public Subject toDomain(SubjectJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Subject.builder()
                .subjectId(entity.getSubjectId())
                .subjectCode(entity.getSubjectCode())
                .subjectName(entity.getSubjectName())
                .description(entity.getDescription())
                .credits(entity.getCredits())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SubjectJpaEntity toEntity(Subject domain) {
        if (domain == null) {
            return null;
        }
        return SubjectJpaEntity.builder()
                .subjectId(domain.getSubjectId() != null ? domain.getSubjectId() : UUID.randomUUID())
                .subjectCode(domain.getSubjectCode())
                .subjectName(domain.getSubjectName())
                .description(domain.getDescription())
                .credits(domain.getCredits() != null ? domain.getCredits() : 3)
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }
}
