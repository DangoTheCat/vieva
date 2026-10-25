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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM SubjectJpaEntity s WHERE s.subjectId=:id")
    Optional<SubjectJpaEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
    Optional<SubjectJpaEntity> findBySubjectCode(String subjectCode);
    boolean existsBySubjectCode(String subjectCode);
    List<SubjectJpaEntity> findByStatus(SubjectStatus status);
}
