package com.example.vieva.application.settings;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * UC1.1 upload limits, bound from {@code vieva.documents.*}.
 */
@Data
public class DocumentSettings {
    private long maxSizeBytes = 20L * 1024 * 1024;
    /** Allowed extensions; each must map to a known MIME type (pdf, docx, pptx, txt). */
    private List<String> allowedExtensions = new ArrayList<>(List.of("pdf", "docx", "pptx", "txt"));
    /** Indexing attempts per document, the first one included. */
    private int maxIndexAttempts = 3;
    /** Below this many extracted characters the document counts as empty (e.g. scanned PDF). */
    private int minTextChars = 50;
}
