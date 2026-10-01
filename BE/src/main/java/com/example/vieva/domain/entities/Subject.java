package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Danh mục môn học trong cơ sở giáo dục.
 * Rich Domain Entity encapsulating subject lifecycle and modification invariants.
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

    public void deactivate() {
        if (this.status == SubjectStatus.ARCHIVED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        this.status = SubjectStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void update(String newName, String newDescription, Integer newCredits, SubjectStatus newStatus) {
        if (newName != null && !newName.trim().isEmpty()) {
            this.subjectName = newName.trim();
        }
        if (newDescription != null) {
            this.description = newDescription.trim();
        }
        if (newCredits != null && newCredits > 0) {
            this.credits = newCredits;
        }
        if (newStatus != null) {
            this.status = newStatus;
        }
        this.updatedAt = Instant.now();
    }
}
