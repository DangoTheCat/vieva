package com.example.vieva.domain.entities;

import lombok.Getter;

@Getter
public enum LanguageLocale {
    VI_VN("vi-VN"),
    EN_US("en-US");

    private final String code;

    LanguageLocale(String code) {
        this.code = code;
    }

    /**
     * Resolves a locale code. A {@code null} or blank input means "no locale" and returns
     * {@code null}; any other unrecognized code is treated as a data error and throws.
     */
    public static LanguageLocale fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim();
        for (LanguageLocale locale : values()) {
            if (locale.code.equalsIgnoreCase(normalized)) {
                return locale;
            }
        }
        throw new IllegalArgumentException("Unknown LanguageLocale code: " + code);
    }
}
