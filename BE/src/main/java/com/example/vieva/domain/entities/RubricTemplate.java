package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RubricTemplate {
    private UUID id;
    private UUID subjectId;
    private String name;
    private String description;
    private String criteriaJson;
    private Instant createdAt;
    private Instant updatedAt;

    public void update(String name, String description, String criteriaJson) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        if (criteriaJson != null && !criteriaJson.isBlank()) {
            this.criteriaJson = criteriaJson.trim();
        }
        this.updatedAt = Instant.now();
    }
}
