package com.example.vieva.application.ports.input.lecturer;

import com.example.vieva.domain.entities.LecturerSubject;
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
public class LecturerSubjectResult {
    private UUID lecturerSubjectId;
    private UUID lecturerId;
    private UUID subjectId;
    private Boolean isActive;
    private Instant assignedAt;
    private UUID assignedBy;
    private Instant revokedAt;

    public static LecturerSubjectResult from(LecturerSubject entity) {
        if (entity == null) {
            return null;
        }
        return LecturerSubjectResult.builder()
                .lecturerSubjectId(entity.getLecturerSubjectId())
                .lecturerId(entity.getLecturerId())
                .subjectId(entity.getSubjectId())
                .isActive(entity.getIsActive())
                .assignedAt(entity.getAssignedAt())
                .assignedBy(entity.getAssignedBy())
                .revokedAt(entity.getRevokedAt())
                .build();
    }
}
