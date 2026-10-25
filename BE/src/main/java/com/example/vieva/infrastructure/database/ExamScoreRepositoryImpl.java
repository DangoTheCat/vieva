package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.domain.entities.ExamScore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ExamScoreRepositoryImpl implements ExamScoreRepository {

    private final ExamScoreJpaRepository jpaRepository;
    private final ExamScorePersistenceMapper mapper;

    @Override
    public ExamScore save(ExamScore score) {
        if (score == null) {
            return null;
        }
        boolean exists = score.getId() != null && jpaRepository.existsById(score.getId());
        ExamScoreJpaEntity entity = mapper.toEntity(score);
        entity.setNewEntity(!exists);
        ExamScoreJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExamScore> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ExamScore> findByAttemptId(UUID attemptId) {
        if (attemptId == null) {
            return Optional.empty();
        }
        return jpaRepository.findByAttemptId(attemptId).map(mapper::toDomain);
    }

    @Override
    public java.util.List<ExamScore> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}
