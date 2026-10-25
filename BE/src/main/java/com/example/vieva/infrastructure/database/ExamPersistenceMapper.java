package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.Exam;
import org.springframework.stereotype.Component;

@Component
public class ExamPersistenceMapper {

    public ExamJpaEntity toEntity(Exam domain) {
        if (domain == null) {
            return null;
        }
        return ExamJpaEntity.builder()
                .id(domain.getId())
                .subjectId(domain.getSubjectId())
                .creatorId(domain.getCreatorId())
                .title(domain.getTitle())
                .durationSeconds(domain.getDurationSeconds())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .isNewEntity(false)
                .build();
    }

    public Exam toDomain(ExamJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Exam.builder()
                .id(entity.getId())
                .subjectId(entity.getSubjectId())
                .creatorId(entity.getCreatorId())
                .title(entity.getTitle())
                .durationSeconds(entity.getDurationSeconds())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
