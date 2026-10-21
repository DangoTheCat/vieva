package com.example.vieva.application.exception;

/**
 * A validation failure bound to one field of the request/aggregate.
 * Carried by {@link ApplicationException}.
 */
public record FieldViolation(String field, ErrorCode code, String message) {

    public static FieldViolation of(String field, ErrorCode code, String message) {
        return new FieldViolation(field, code, message);
    }
}
