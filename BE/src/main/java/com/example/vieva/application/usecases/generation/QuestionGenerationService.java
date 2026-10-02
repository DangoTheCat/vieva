package com.example.vieva.application.usecases.generation;

import com.example.vieva.application.ports.input.GenerateQuestionsCommand;
import com.example.vieva.application.ports.output.GenerationResult;
import com.example.vieva.application.ports.output.QuestionVersionView;

import java.util.UUID;

/**
 * UC1.2 / WF01 steps 5-7: retrieve context from READY documents of the subject, let the LLM draft
 * questions + rubrics + sources, validate them and store the valid ones as AI DRAFTs.
 */
public interface QuestionGenerationService {
    GenerationResult generate(GenerateQuestionsCommand command, UUID actorId);

    /** Retries a FAILED/PARTIAL request for the missing questions, within the attempt budget. */
    GenerationResult retry(UUID generationRequestId, UUID actorId);

    GenerationResult getRequest(UUID generationRequestId);

    /**
     * Regenerates ONE AI draft from the stored context and the lecturer's feedback; other drafts are
     * untouched. The draft is left unchanged when no valid replacement is produced.
     */
    QuestionVersionView regenerate(UUID versionId, String feedback, Long expectedVersion, UUID actorId);
}
