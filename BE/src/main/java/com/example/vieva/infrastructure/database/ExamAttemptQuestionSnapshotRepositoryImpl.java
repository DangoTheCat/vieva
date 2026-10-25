package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ExamAttemptQuestionSnapshotRepository;
import com.example.vieva.domain.entities.ExamAttemptQuestionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExamAttemptQuestionSnapshotRepositoryImpl implements ExamAttemptQuestionSnapshotRepository {

    private final ExamAttemptQuestionSnapshotJpaRepository jpaRepository;
    private final ExamAttemptQuestionSnapshotPersistenceMapper mapper;

    @Override
    public ExamAttemptQuestionSnapshot save(ExamAttemptQuestionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        boolean exists = snapshot.getId() != null && jpaRepository.existsById(snapshot.getId());
        ExamAttemptQuestionSnapshotJpaEntity entity = mapper.toEntity(snapshot);
        entity.setNewEntity(!exists);
        ExamAttemptQuestionSnapshotJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ExamAttemptQuestionSnapshot> saveAll(List<ExamAttemptQuestionSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        List<ExamAttemptQuestionSnapshotJpaEntity> entities = snapshots.stream()
                .map(s -> {
                    boolean exists = s.getId() != null && jpaRepository.existsById(s.getId());
                    ExamAttemptQuestionSnapshotJpaEntity entity = mapper.toEntity(s);
                    entity.setNewEntity(!exists);
                    return entity;
                })
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ExamAttemptQuestionSnapshot> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ExamAttemptQuestionSnapshot> findByAttemptIdOrderByOrderIndexAsc(UUID attemptId) {
        if (attemptId == null) {
            return List.of();
        }
        return jpaRepository.findByAttemptIdOrderByOrderIndexAsc(attemptId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ExamAttemptQuestionSnapshot> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId) {
        if (attemptId == null || questionId == null) {
            return Optional.empty();
        }
        return jpaRepository.findByAttemptIdAndQuestionId(attemptId, questionId).map(mapper::toDomain);
    }
}
