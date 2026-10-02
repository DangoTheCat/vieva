package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.domain.entities.LecturerSubject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LecturerSubjectRepositoryImpl implements LecturerSubjectRepository {

    private final LecturerSubjectJpaRepository jpaRepository;
    private final LecturerSubjectPersistenceMapper mapper;

    @Override
    @Transactional
    public LecturerSubject save(LecturerSubject assignment) {
        LecturerSubjectJpaEntity entity = mapper.toEntity(assignment);
        LecturerSubjectJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public List<LecturerSubject> saveAll(List<LecturerSubject> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }
        List<LecturerSubjectJpaEntity> entities = assignments.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LecturerSubject> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LecturerSubject> findActiveAssignment(UUID lecturerId, UUID subjectId) {
        return jpaRepository.findByLecturerIdAndSubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(lecturerId, subjectId)
                .map(mapper::toDomain);
    }

    @Override
    public List<LecturerSubject> findActiveAssignments(UUID subjectId, Collection<UUID> lecturerIds) {
        if (subjectId == null || lecturerIds == null || lecturerIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository
                .findBySubject_SubjectIdAndLecturerIdInAndIsActiveTrueAndRevokedAtIsNull(subjectId, lecturerIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LecturerSubject> findActiveBySubjectId(UUID subjectId) {
        return jpaRepository.findBySubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(subjectId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LecturerSubject> findActiveByLecturerId(UUID lecturerId) {
        return jpaRepository.findByLecturerIdAndIsActiveTrueAndRevokedAtIsNull(lecturerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isLecturerAssignedToSubject(UUID lecturerId, UUID subjectId) {
        return jpaRepository.existsByLecturerIdAndSubject_SubjectIdAndIsActiveTrueAndRevokedAtIsNull(lecturerId, subjectId);
    }
}
