package com.example.vieva.application.ports.input.document;

import java.util.List;

/**
 * Preview of a document and its first few chunks for the lecturer UI.
 */
public record DocumentPreview(
        CourseDocumentResult document,
        List<PreviewChunk> chunks
) {
    public record PreviewChunk(String label, String text, boolean truncated) {
    }
}
