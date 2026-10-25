package com.example.vieva.application.ports.input.question;

import java.util.List;
import java.util.UUID;

public interface RubricTemplateService {
    RubricTemplateResult createTemplate(CreateRubricTemplateCommand cmd, UUID actorId);
    RubricTemplateResult updateTemplate(UUID templateId, UpdateRubricTemplateCommand cmd, UUID actorId);
    RubricTemplateResult getTemplateById(UUID templateId);
    List<RubricTemplateResult> getTemplatesBySubject(UUID subjectId);
    void deleteTemplate(UUID templateId, UUID actorId);
    RubricResult copyTemplateToQuestionVersion(UUID templateId, UUID versionId, UUID actorId);
}
