package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.CourseDocument;
import java.util.List;

/**
 * Preview of a document and its first few chunks for the lecturer UI.
 */
public record DocumentPreview(
        CourseDocument document,
        List<PreviewChunk> chunks
) {
    public record PreviewChunk(String label, String text, boolean truncated) {
    }
}
