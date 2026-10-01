package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Danh mục môn học trong cơ sở giáo dục.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private String description;
    private Integer credits;
    private SubjectStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
