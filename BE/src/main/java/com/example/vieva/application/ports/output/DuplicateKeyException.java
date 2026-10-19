package com.example.vieva.application.ports.output;

/**
 * Thrown by repository output ports when a unique constraint / duplicate key violation occurs.
 * Protects use cases from depending on vendor-specific or framework exceptions (e.g. DataIntegrityViolationException).
 */
public class DuplicateKeyException extends RuntimeException {

    public DuplicateKeyException(String message) {
        super(message);
    }

    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
