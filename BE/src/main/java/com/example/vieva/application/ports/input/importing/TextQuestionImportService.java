package com.example.vieva.application.ports.input.importing;

import java.util.UUID;

public interface TextQuestionImportService {
    TextQuestionImportResult importTextQuestions(TextQuestionImportCommand command, UUID actorId);
}
