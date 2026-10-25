package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ExamRepository;
import com.example.vieva.domain.entities.Exam;
import com.example.vieva.domain.entities.ExamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExamRepositoryImpl implements ExamRepository {

    private final ExamJpaRepository jpaRepository;
    private final ExamPersistenceMapper mapper;

    @Override
    public Exam save(Exam exam) {
        if (exam == null) {
            return null;
        }
        boolean exists = exam.getId() != null && jpaRepository.existsById(exam.getId());
        ExamJpaEntity entity = mapper.toEntity(exam);
        entity.setNewEntity(!exists);
        ExamJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Exam> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Exam> findByIdForUpdate(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<Exam> findBySubjectId(UUID subjectId) {
        if (subjectId == null) {
            return List.of();
        }
        return jpaRepository.findBySubjectId(subjectId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Exam> findByStatus(ExamStatus status) {
        if (status == null) {
            return List.of();
        }
        return jpaRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        if (id != null) {
            jpaRepository.deleteById(id);
        }
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByStatus(ExamStatus status) {
        if (status == null) {
            return 0;
        }
        return jpaRepository.countByStatus(status);
    }
}
