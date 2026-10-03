package com.example.vieva.domain.exception;

/**
 * A validation failure bound to one field of the request/aggregate, e.g.
 * {@code rubric.totalPoints / RUBRIC_SCORE_MISMATCH}. Carried by {@link AppException}.
 */
public record FieldViolation(String field, ErrorCode code, String message) {

    public static FieldViolation of(String field, ErrorCode code, String message) {
        return new FieldViolation(field, code, message);
    }
}
