package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.QuestionGenerationRequestRepository;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class QuestionGenerationRequestRepositoryImpl implements QuestionGenerationRequestRepository {

    private final QuestionGenerationRequestJpaRepository jpaRepository;
    private final QuestionGenerationRequestPersistenceMapper mapper;

    @Override
    public QuestionGenerationRequest save(QuestionGenerationRequest request) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(request)));
    }

    @Override
    public Optional<QuestionGenerationRequest> findById(UUID generationRequestId) {
        return jpaRepository.findById(generationRequestId).map(mapper::toDomain);
    }
}
