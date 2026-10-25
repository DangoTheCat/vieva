package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.ExamQuestionRepository;
import com.example.vieva.domain.entities.ExamQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExamQuestionRepositoryImpl implements ExamQuestionRepository {

    private final ExamQuestionJpaRepository jpaRepository;
    private final ExamQuestionPersistenceMapper mapper;

    @Override
    public ExamQuestion save(ExamQuestion examQuestion) {
        if (examQuestion == null) {
            return null;
        }
        boolean exists = examQuestion.getId() != null && jpaRepository.existsById(examQuestion.getId());
        ExamQuestionJpaEntity entity = mapper.toEntity(examQuestion);
        entity.setNewEntity(!exists);
        ExamQuestionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ExamQuestion> saveAll(List<ExamQuestion> examQuestions) {
        if (examQuestions == null || examQuestions.isEmpty()) {
            return List.of();
        }
        List<ExamQuestionJpaEntity> entities = examQuestions.stream()
                .map(eq -> {
                    boolean exists = eq.getId() != null && jpaRepository.existsById(eq.getId());
                    ExamQuestionJpaEntity entity = mapper.toEntity(eq);
                    entity.setNewEntity(!exists);
                    return entity;
                })
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamQuestion> findByExamIdOrderByOrderIndexAsc(UUID examId) {
        if (examId == null) {
            return List.of();
        }
        return jpaRepository.findByExamIdOrderByOrderIndexAsc(examId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByExamId(UUID examId) {
        if (examId != null) {
            jpaRepository.deleteByExamId(examId);
        }
    }
}
