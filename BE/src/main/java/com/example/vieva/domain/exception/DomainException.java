package com.example.vieva.domain.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class DomainException extends RuntimeException {

    private final DomainErrorCode errorCode;
    private final List<FieldViolation> violations;

    public DomainException(DomainErrorCode errorCode) {
        this(errorCode, errorCode != null ? errorCode.getMessage() : null, List.of());
    }

    public DomainException(DomainErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public DomainException(DomainErrorCode errorCode, String message, List<FieldViolation> violations) {
        super(message);
        this.errorCode = errorCode;
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public static DomainException ofViolations(List<FieldViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
        FieldViolation first = violations.get(0);
        String message = violations.size() == 1
                ? first.message()
                : first.message() + " (+" + (violations.size() - 1) + " more)";
        return new DomainException(first.code(), message, violations);
    }
}
