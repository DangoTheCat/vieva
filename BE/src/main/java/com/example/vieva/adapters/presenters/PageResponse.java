package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.common.PageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;

    public static <T, R> PageResponse<R> from(PageResult<T> page, Function<T, R> mapper) {
        if (page == null) return null;
        return PageResponse.<R>builder()
                .content(page.getContent() == null ? List.of() : page.getContent().stream().map(mapper).toList())
                .page(page.getPage())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
