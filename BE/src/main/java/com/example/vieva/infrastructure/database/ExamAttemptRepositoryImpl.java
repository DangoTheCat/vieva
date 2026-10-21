package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExamAttemptRepositoryImpl implements ExamAttemptRepository {

    private final JpaExamAttemptRepository jpaRepository;
    private final ExamAttemptPersistenceMapper mapper;

    @Override
    public ExamAttempt save(ExamAttempt attempt) {
        if (attempt == null) {
            return null;
        }
        boolean exists = attempt.getId() != null && jpaRepository.existsById(attempt.getId());
        ExamAttemptJpaEntity entity = mapper.toEntity(attempt);
        entity.setNewEntity(!exists);
        ExamAttemptJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExamAttempt> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ExamAttempt> findByStudentAndStatus(UUID studentId, AttemptStatus status) {
        if (studentId == null || status == null) {
            return List.of();
        }
        return jpaRepository.findByStudentIdAndStatus(studentId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamAttempt> findByStudentId(UUID studentId) {
        if (studentId == null) {
            return List.of();
        }
        return jpaRepository.findByStudentIdOrderByStartedAtDesc(studentId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
