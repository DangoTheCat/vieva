package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.BloomLevel;
import lombok.Builder;

import java.util.UUID;

/**
 * UC1.3 edit of a DRAFT. Null fields are left unchanged; a non-null rubric replaces the whole rubric.
 *
 * @param bloomConfirmed  explicit Bloom confirmation by the lecturer (BR-01)
 * @param expectedVersion lock token the client last saw; stale tokens are rejected (BR-08)
 */
@Builder
public record UpdateDraftCommand(
        UUID topicId,
        String content,
        String expectedAnswer,
        BloomLevel bloomLevel,
        Boolean bloomConfirmed,
        RubricInput rubric,
        Long expectedVersion
) {
}
