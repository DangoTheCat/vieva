package com.example.vieva.application.ports.output;

import java.util.List;

/**
 * Splits extracted sections into overlapping, token-bounded chunks (sizes come from configuration).
 */
public interface TextSplitterPort {
    List<TextChunk> split(List<ParsedSection> sections);
}
