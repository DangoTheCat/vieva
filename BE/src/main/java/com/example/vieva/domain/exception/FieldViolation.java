package com.example.vieva.domain.exception;

/**
 * A validation failure bound to one field of the domain aggregate.
 * Carried by {@link DomainException}.
 */
public record FieldViolation(String field, DomainErrorCode code, String message) {

    public static FieldViolation of(String field, DomainErrorCode code, String message) {
        return new FieldViolation(field, code, message);
    }
}
