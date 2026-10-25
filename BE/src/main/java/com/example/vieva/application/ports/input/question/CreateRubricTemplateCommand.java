package com.example.vieva.application.ports.input.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRubricTemplateCommand {
    private UUID subjectId;
    private String name;
    private String description;
    private String criteriaJson;
}
