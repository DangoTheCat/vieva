package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CreateManualQuestionCommand;
import com.example.vieva.application.ports.input.UpdateDraftCommand;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.domain.entities.BloomLevel;

import java.util.UUID;

/**
 * Lecturer authoring of drafts: UC1.5 (manual create), UC1.3 (edit / delete draft, confirm Bloom)
 * and Copy-on-Write of an approved question into a new draft.
 */
public interface QuestionAuthoringService {
    QuestionVersionView createManualQuestion(UUID subjectId, CreateManualQuestionCommand command, UUID actorId);

    QuestionVersionView updateDraft(UUID versionId, UpdateDraftCommand command, UUID actorId);

    QuestionVersionView confirmBloom(UUID versionId, BloomLevel bloomLevel, Long expectedVersion, UUID actorId);

    void deleteDraft(UUID versionId, UUID actorId);

    /** Copy-on-Write: new DRAFT from the approved version in force; the approved one stays untouched. */
    QuestionVersionView createDraftFromApproved(UUID questionId, UUID actorId);
}
