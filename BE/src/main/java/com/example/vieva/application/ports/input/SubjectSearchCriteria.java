package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.SubjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSearchCriteria {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "subjectCode", "subjectName", "credits", "status"
    );

    private String keyword;
    private SubjectStatus status;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "DESC";

    public String getSanitizedSortBy() {
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy.trim())) {
            return "createdAt";
        }
        return sortBy.trim();
    }

    public int getSanitizedSize() {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }

    public int getSanitizedPage() {
        return Math.max(page, 0);
    }

    /**
     * Escape SQL LIKE special characters (% and _) to avoid wildcard injections.
     */
    public String getEscapedKeyword() {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
