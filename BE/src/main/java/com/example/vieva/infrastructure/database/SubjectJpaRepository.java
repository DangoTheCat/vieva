package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectJpaRepository extends JpaRepository<SubjectJpaEntity, UUID>, JpaSpecificationExecutor<SubjectJpaEntity> {
    Optional<SubjectJpaEntity> findBySubjectCode(String subjectCode);
    boolean existsBySubjectCode(String subjectCode);
    List<SubjectJpaEntity> findByStatusOrderBySubjectCodeAsc(SubjectStatus status);
}
