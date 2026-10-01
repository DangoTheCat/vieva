package com.example.vieva.infrastructure.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Phân công giảng viên phụ trách môn học. Là cơ sở kiểm tra RBAC phạm vi môn (Course-scoped RBAC).
 */
@Entity
// The (lecturer_id, subject_id) uniqueness is a PARTIAL index managed by Flyway:
//   CREATE UNIQUE INDEX uq_lecturer_subject ... WHERE revoked_at IS NULL
// so that a revoked assignment (revoked_at != null) can be re-created later.
@Table(
    name = "lecturer_subjects",
    indexes = {
        @Index(name = "idx_lecturer_subjects_lookup", columnList = "lecturer_id, subject_id, is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LecturerSubjectJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "lecturer_subject_id", updatable = false, nullable = false)
    private UUID lecturerSubjectId;

    @Column(name = "lecturer_id", nullable = false)
    private UUID lecturerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectJpaEntity subject;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return lecturerSubjectId;
    }

    @Override
    public boolean isNew() {
        return isNew || assignedAt == null;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
