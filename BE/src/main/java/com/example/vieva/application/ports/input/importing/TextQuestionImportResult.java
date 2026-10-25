package com.example.vieva.application.ports.input.importing;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TextQuestionImportResult {
    int totalImported;
    List<UUID> questionIds;
}
