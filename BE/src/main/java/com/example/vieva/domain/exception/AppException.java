package com.example.vieva.domain.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldViolation> violations;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), List.of());
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public AppException(ErrorCode errorCode, String message, List<FieldViolation> violations) {
        super(message);
        this.errorCode = errorCode;
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    /** Field-level failure: the top-level code is the first violation's code. */
    public static AppException ofViolations(List<FieldViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
        FieldViolation first = violations.get(0);
        String message = violations.size() == 1
                ? first.message()
                : first.message() + " (+" + (violations.size() - 1) + " more)";
        return new AppException(first.code(), message, violations);
    }
}
