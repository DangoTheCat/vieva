package com.example.vieva.application.ports.input.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResult {
    private UUID subjectId;
    private UUID headLecturerId;
    private String subjectCode;
    private String subjectName;
    private String description;
    private Integer credits;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
