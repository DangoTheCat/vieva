package com.example.vieva.application.ports.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java generic model representing paginated results in the Application layer.
 * Decoupled from Spring Data Page interface.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {

    @Builder.Default
    private List<T> content = Collections.emptyList();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;

    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PagedResult.<T>builder()
                .content(content != null ? content : Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isFirst(page <= 0)
                .isLast(page >= totalPages - 1 || totalPages == 0)
                .build();
    }
}
