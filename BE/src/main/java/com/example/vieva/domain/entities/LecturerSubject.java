package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Phân công giảng viên phụ trách môn học. Là cơ sở kiểm tra RBAC phạm vi môn (Course-scoped RBAC).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerSubject {
    private UUID lecturerSubjectId;
    private UUID lecturerId;
    private UUID subjectId;
    private Boolean isActive;
    private Instant assignedAt;
    private UUID assignedBy;
    private Instant revokedAt;

    public void revoke() {
        this.isActive = false;
        this.revokedAt = Instant.now();
    }
}
