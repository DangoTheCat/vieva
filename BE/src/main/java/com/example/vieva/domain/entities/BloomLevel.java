package com.example.vieva.domain.entities;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Bloom's taxonomy cognitive levels (revised, 6 levels) — BR-01.
 */
public enum BloomLevel {
    REMEMBER("Nhớ"),
    UNDERSTAND("Hiểu"),
    APPLY("Vận dụng"),
    ANALYZE("Phân tích"),
    EVALUATE("Đánh giá"),
    CREATE("Sáng tạo");

    private final String viLabel;

    BloomLevel(String viLabel) {
        this.viLabel = viLabel;
    }

    public String getViLabel() {
        return viLabel;
    }

    /**
     * Resolves a level from its code ({@code APPLY}) or its Vietnamese label ({@code "Vận dụng"}),
     * ignoring case, surrounding whitespace and diacritics. Used by import and LLM output parsing.
     */
    public static Optional<BloomLevel> fromCodeOrLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(raw);
        for (BloomLevel level : values()) {
            if (normalize(level.name()).equals(key) || normalize(level.viLabel).equals(key)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }

    private static String normalize(String value) {
        String stripped = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return stripped.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
    }
}
