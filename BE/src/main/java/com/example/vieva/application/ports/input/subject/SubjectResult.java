package com.example.vieva.application.ports.input.subject;

import com.example.vieva.domain.entities.Subject;
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

    public static SubjectResult from(Subject subject) {
        if (subject == null) {
            return null;
        }
        return SubjectResult.builder()
                .subjectId(subject.getSubjectId())
                .headLecturerId(subject.getHeadLecturerId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .description(subject.getDescription())
                .credits(subject.getCredits())
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
