package com.example.vieva.application.ports.input.question;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class RubricTemplateResult {
    UUID id;
    UUID subjectId;
    String name;
    String description;
    String criteriaJson;
    Instant createdAt;
    Instant updatedAt;
}
