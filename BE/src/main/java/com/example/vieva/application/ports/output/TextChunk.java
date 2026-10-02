package com.example.vieva.application.ports.output;

/**
 * @param pageStart first page covered by the chunk ({@code null} when unknown)
 * @param pageEnd   last page covered by the chunk ({@code null} when unknown)
 */
public record TextChunk(String text, int tokenCount, Integer pageStart, Integer pageEnd, String sectionTitle) {
}
