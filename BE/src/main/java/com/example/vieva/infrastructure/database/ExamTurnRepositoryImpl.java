package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.domain.entities.ExamTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExamTurnRepositoryImpl implements ExamTurnRepository {

    private final ExamTurnJpaRepository jpaRepository;
    private final ExamTurnPersistenceMapper mapper;

    @Override
    public ExamTurn save(ExamTurn turn) {
        if (turn == null) {
            return null;
        }
        boolean exists = turn.getId() != null && jpaRepository.existsById(turn.getId());
        ExamTurnJpaEntity entity = mapper.toEntity(turn);
        entity.setNewEntity(!exists);
        ExamTurnJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExamTurn> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ExamTurn> findByAttemptIdOrderByTurnOrderAsc(UUID attemptId) {
        if (attemptId == null) {
            return List.of();
        }
        return jpaRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamTurn> findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(UUID attemptId, UUID questionId) {
        if (attemptId == null || questionId == null) {
            return List.of();
        }
        return jpaRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(attemptId, questionId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
