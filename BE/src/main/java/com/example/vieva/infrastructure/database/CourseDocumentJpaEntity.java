package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.DocumentIndexingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Tài liệu giáo trình/slide do GV tải lên; chỉ READY mới được dùng cho RAG.
 */
@Entity
@Table(
    name = "course_documents",
    indexes = {
        @Index(name = "idx_documents_subject_status", columnList = "subject_id, indexing_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDocumentJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "document_id", updatable = false, nullable = false)
    private UUID documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectJpaEntity subject;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "indexing_status", nullable = false, length = 30)
    private DocumentIndexingStatus indexingStatus = DocumentIndexingStatus.UPLOADED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "extracted_text_summary", columnDefinition = "TEXT")
    private String extractedTextSummary;

    @Builder.Default
    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return documentId;
    }

    @Override
    public boolean isNew() {
        return isNew || createdAt == null;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
