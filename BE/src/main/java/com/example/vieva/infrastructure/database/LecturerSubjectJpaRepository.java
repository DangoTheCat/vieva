package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LecturerSubjectJpaRepository extends JpaRepository<LecturerSubjectJpaEntity, UUID> {

    Optional<LecturerSubjectJpaEntity> findByLecturerIdAndSubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(UUID lecturerId, UUID subjectId);

    List<LecturerSubjectJpaEntity> findBySubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(UUID subjectId);

    List<LecturerSubjectJpaEntity> findByLecturerIdAndIsActiveTrueAndRevokedAtIsNull(UUID lecturerId);

    boolean existsByLecturerIdAndSubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(UUID lecturerId, UUID subjectId);
}
