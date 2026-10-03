package com.example.vieva.application.ports.output;

/**
 * Text of one page/slide/section.
 *
 * @param pageNumber   1-based page or slide number, {@code null} when the format has no pages
 * @param sectionTitle nearest heading, {@code null} when unknown
 */
public record ParsedSection(String text, Integer pageNumber, String sectionTitle) {
}
