package com.example.vieva.infrastructure.web;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("Application exception [{}]: {}", errorCode.getCode(), errorCode.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(toHttpStatus(errorCode)).body(response);
    }

    /**
     * Handles DB-level unique constraint violations that slip past the application-layer
     * existsBy* check in a concurrent scenario (two requests racing to create the same email).
     * The DB partial unique index is the last line of defence — we surface it as 400 USER_EXISTED
     * rather than letting it propagate as a 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String msg = exception.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation: {}", msg);

        // Detect duplicate email / user_code constraint names defined in V0 migration
        if (msg != null && (msg.contains("uq_users_active_email") || msg.contains("uq_users_active_user_code")
                || msg.contains("email") && msg.contains("unique"))) {
            ErrorResponse response = ErrorResponse.builder()
                    .code(ErrorCode.USER_EXISTED.getCode())
                    .message(ErrorCode.USER_EXISTED.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Generic constraint violation — expose as 409 Conflict
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message("Request conflicts with an existing resource")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles JPA optimistic locking failures (caused by @Version).
     * This fires when two concurrent requests try to modify the same row (e.g. both
     * attempting to lock/demote the last admin simultaneously).
     * Returns 409 Conflict so the client can retry.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException exception) {
        log.warn("Optimistic locking conflict on {}: {}", exception.getPersistentClassName(), exception.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .code("4090")
                .message("Request conflict — resource was modified concurrently. Please retry.")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ErrorResponse response = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String defaultMessage = exception.getBindingResult().getFieldError() != null
                ? exception.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.INVALID_REQUEST.getMessage();

        log.warn("Validation failed: {}", defaultMessage);
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message(defaultMessage)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("Invalid argument: {}", exception.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message(exception.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        // Log the full stack trace so it appears in application logs for diagnosis
        log.error("Unhandled exception: {}", exception.getMessage(), exception);
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus toHttpStatus(ErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case USER_NOT_FOUND, SUBJECT_NOT_FOUND, ASSIGNMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case UNAUTHORIZED, USER_INACTIVE -> HttpStatus.FORBIDDEN;
            case UNCATEGORIZED_EXCEPTION -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
