package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.LecturerSubject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LecturerSubjectPersistenceMapper {

    private final SubjectJpaRepository subjectJpaRepository;

    public LecturerSubject toDomain(LecturerSubjectJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return LecturerSubject.builder()
                .lecturerSubjectId(entity.getLecturerSubjectId())
                .lecturerId(entity.getLecturerId())
                .subjectId(entity.getSubject() != null ? entity.getSubject().getSubjectId() : null)
                .isActive(entity.getIsActive())
                .assignedAt(entity.getAssignedAt())
                .assignedBy(entity.getAssignedBy())
                .revokedAt(entity.getRevokedAt())
                .build();
    }

    public LecturerSubjectJpaEntity toEntity(LecturerSubject domain) {
        if (domain == null) {
            return null;
        }

        SubjectJpaEntity subjectEntity = domain.getSubjectId() != null
                ? subjectJpaRepository.findById(domain.getSubjectId()).orElse(null)
                : null;

        return LecturerSubjectJpaEntity.builder()
                .lecturerSubjectId(domain.getLecturerSubjectId() != null ? domain.getLecturerSubjectId() : UUID.randomUUID())
                .lecturerId(domain.getLecturerId())
                .subject(subjectEntity)
                .isActive(domain.getIsActive() != null ? domain.getIsActive() : true)
                .assignedAt(domain.getAssignedAt())
                .assignedBy(domain.getAssignedBy())
                .revokedAt(domain.getRevokedAt())
                .isNew(domain.getAssignedAt() == null)
                .build();
    }
}
