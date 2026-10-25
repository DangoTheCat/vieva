package com.example.vieva.application.ports.input.importing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextQuestionImportCommand {
    private UUID subjectId;
    private String text;
    private UUID topicId;
    private String bloomLevel;
    private UUID rubricTemplateId;
}
