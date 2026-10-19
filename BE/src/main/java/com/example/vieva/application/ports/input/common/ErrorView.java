package com.example.vieva.application.ports.input.common;

import com.example.vieva.application.exception.ErrorCategory;

import java.util.List;

public record ErrorView(
        String code,
        String error,
        String message,
        ErrorCategory category,
        List<FieldError> errors
) {
    public record FieldError(String field, String error, String message) {
    }
}
