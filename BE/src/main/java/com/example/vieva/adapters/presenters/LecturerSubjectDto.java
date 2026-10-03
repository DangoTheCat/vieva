package com.example.vieva.adapters.presenters;

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
public class LecturerSubjectDto {
    private UUID lecturerSubjectId;
    private UUID lecturerId;
    private UUID subjectId;
    private Boolean isActive;
    private Instant assignedAt;
    private UUID assignedBy;
    private Instant revokedAt;
}
