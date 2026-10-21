package com.example.vieva.application.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldViolation> violations;

    public ApplicationException(ErrorCode errorCode) {
        this(errorCode, errorCode != null ? errorCode.getMessage() : null, (Throwable) null, List.of());
    }

    public ApplicationException(ErrorCode errorCode, String message) {
        this(errorCode, message, (Throwable) null, List.of());
    }

    public ApplicationException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, errorCode != null ? errorCode.getMessage() : null, cause, List.of());
    }

    public ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, List.of());
    }

    public ApplicationException(ErrorCode errorCode, String message, List<FieldViolation> violations) {
        this(errorCode, message, null, violations);
    }

    public ApplicationException(ErrorCode errorCode, String message, Throwable cause, List<FieldViolation> violations) {
        super(message, cause);
        this.errorCode = errorCode;
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    /** Field-level failure: the top-level code is the first violation's code. */
    public static ApplicationException ofViolations(List<FieldViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
        FieldViolation first = violations.get(0);
        String message = violations.size() == 1
                ? first.message()
                : first.message() + " (+" + (violations.size() - 1) + " more)";
        return new ApplicationException(first.code(), message, violations);
    }
}
