package com.example.vieva.adapters.presenters;

import com.example.vieva.domain.entities.SubjectStatus;
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
public class SubjectDto {
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private String description;
    private Integer credits;
    private SubjectStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
