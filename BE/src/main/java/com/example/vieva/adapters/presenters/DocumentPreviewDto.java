package com.example.vieva.adapters.presenters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPreviewDto {
    private CourseDocumentDto document;
    private List<PreviewChunkDto> chunks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewChunkDto {
        private String label;
        private String text;
        private boolean truncated;
    }
}
