package com.example.vieva.application.ports.output;

import java.util.List;

/**
 * Extracts text from a course document, split into sections that keep page/slide/heading metadata.
 */
public interface DocumentParserPort {
    List<ParsedSection> extract(byte[] content, String mimeType);
}
