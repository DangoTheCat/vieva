package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LecturerSubjectJpaRepository extends JpaRepository<LecturerSubjectJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"subject"})
    Optional<LecturerSubjectJpaEntity> findByLecturerIdAndSubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(UUID lecturerId, UUID subjectId);

    @EntityGraph(attributePaths = {"subject"})
    List<LecturerSubjectJpaEntity> findBySubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(UUID subjectId);

    @EntityGraph(attributePaths = {"subject"})
    List<LecturerSubjectJpaEntity> findByLecturerIdAndIsActiveTrueAndRevokedAtIsNull(UUID lecturerId);

    @EntityGraph(attributePaths = {"subject"})
    List<LecturerSubjectJpaEntity> findBySubject_SubjectIdAndLecturerIdInAndIsActiveTrueAndRevokedAtIsNull(UUID subjectId, Collection<UUID> lecturerIds);

    boolean existsByLecturerIdAndSubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(UUID lecturerId, UUID subjectId);
}
