package com.example.vieva.application.ports.output;

import java.util.Map;

public record ImportRow(int rowNumber, Map<String, String> values) {
    public String get(String column) {
        String value = values.get(column);
        return value == null ? null : value.trim();
    }

    public boolean isBlank() {
        return values.values().stream().allMatch(v -> v == null || v.isBlank());
    }
}
