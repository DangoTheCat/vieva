package com.example.vieva.application.ports.input.generation;

import com.example.vieva.application.ports.input.GenerateQuestionsCommand;
import com.example.vieva.application.ports.output.GenerationResult;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.domain.entities.BloomLevel;

import java.util.List;
import java.util.UUID;

public interface QuestionGenCoordinator {
    QuestionVersionView coordinateRAGPipeline(UUID topicId, BloomLevel difficulty, List<UUID> documentIds, UUID actorId);

    GenerationResult generate(GenerateQuestionsCommand command, UUID actorId);

    GenerationResult retry(UUID generationRequestId, UUID actorId);

    GenerationResult getRequest(UUID generationRequestId);

    QuestionVersionView regenerate(UUID versionId, String feedback, Long expectedVersion, UUID actorId);
}
