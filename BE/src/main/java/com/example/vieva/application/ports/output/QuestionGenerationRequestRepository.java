package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.QuestionGenerationRequest;

import java.util.Optional;
import java.util.UUID;

public interface QuestionGenerationRequestRepository {
    QuestionGenerationRequest save(QuestionGenerationRequest request);
    Optional<QuestionGenerationRequest> findById(UUID generationRequestId);
}
