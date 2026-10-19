package com.example.vieva.application.ports.output;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;

/**
 * Thrown by repository output ports when a unique constraint / duplicate key violation occurs.
 * Protects use cases from depending on vendor-specific or framework exceptions (e.g. DataIntegrityViolationException).
 */
public class DuplicateKeyException extends ApplicationException {

    private final ErrorCode suggestedErrorCode;

    public DuplicateKeyException(String message) {
        this(message, (Throwable) null, null);
    }

    public DuplicateKeyException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public DuplicateKeyException(String message, ErrorCode suggestedErrorCode) {
        this(message, (Throwable) null, suggestedErrorCode);
    }

    public DuplicateKeyException(String message, Throwable cause, ErrorCode suggestedErrorCode) {
        super(suggestedErrorCode != null ? suggestedErrorCode : ErrorCode.INVALID_REQUEST, message, cause);
        this.suggestedErrorCode = suggestedErrorCode;
    }

    public ErrorCode getSuggestedErrorCode() {
        return suggestedErrorCode;
    }
}
