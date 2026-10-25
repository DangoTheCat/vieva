package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RubricTemplateJpaRepository extends JpaRepository<RubricTemplateJpaEntity, UUID> {
    List<RubricTemplateJpaEntity> findBySubjectIdOrderByCreatedAtDesc(UUID subjectId);
}
