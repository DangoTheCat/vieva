package com.example.vieva.infrastructure.web;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("Application exception [{} {}]: {}", errorCode.getCode(), errorCode.name(), exception.getMessage());
        List<ErrorResponse.FieldError> fieldErrors = exception.getViolations().isEmpty() ? null
                : exception.getViolations().stream()
                .map(v -> ErrorResponse.FieldError.builder()
                        .field(v.field())
                        .error(v.code().name())
                        .message(v.message())
                        .build())
                .toList();
        return ResponseEntity.status(toHttpStatus(errorCode))
                .body(body(errorCode, exception.getMessage(), fieldErrors));
    }

    /**
     * DB-level unique constraint violations that slip past application checks in a race.
     * The DB index is the last line of defence — surface it as a client error, never as a 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String msg = exception.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation: {}", msg);

        if (msg != null && (msg.contains("uq_users_active_email") || msg.contains("uq_users_active_user_code")
                || msg.contains("email") && msg.contains("unique"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(body(ErrorCode.USER_EXISTED, ErrorCode.USER_EXISTED.getMessage(), null));
        }
        if (msg != null && msg.contains("uq_question_draft_per_question")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(body(ErrorCode.DRAFT_ALREADY_EXISTS, ErrorCode.DRAFT_ALREADY_EXISTS.getMessage(), null));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(ErrorCode.INVALID_REQUEST, "Request conflicts with an existing resource", null));
    }

    /** {@code @Version} conflicts (BR-08): another request modified the same row first. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException exception) {
        log.warn("Optimistic locking conflict on {}", exception.getPersistentClassName());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(ErrorCode.CONCURRENT_MODIFICATION, ErrorCode.CONCURRENT_MODIFICATION.getMessage(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .error(ErrorCode.INVALID_REQUEST.name())
                        .message(error.getDefaultMessage())
                        .build())
                .toList();
        String message = fieldErrors.isEmpty() ? ErrorCode.INVALID_REQUEST.getMessage() : fieldErrors.get(0).getMessage();
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(ErrorCode.INVALID_REQUEST, message, fieldErrors.isEmpty() ? null : fieldErrors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        String message = "Malformed request body";
        if (exception instanceof MethodArgumentTypeMismatchException e) {
            message = "Invalid value for parameter '" + e.getName() + "'";
        } else if (exception instanceof MissingServletRequestParameterException e) {
            message = "Missing parameter '" + e.getParameterName() + "'";
        } else if (exception instanceof MissingServletRequestPartException e) {
            message = "Missing multipart part '" + e.getRequestPartName() + "'";
        }
        log.warn("Malformed request: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(ErrorCode.INVALID_REQUEST, message, null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(body(ErrorCode.FILE_TOO_LARGE, ErrorCode.FILE_TOO_LARGE.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("Invalid argument: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(ErrorCode.INVALID_REQUEST, exception.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        // Full stack trace goes to the log only — never to the client.
        log.error("Unhandled exception: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(ErrorCode.UNCATEGORIZED_EXCEPTION, ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage(), null));
    }

    private static ErrorResponse body(ErrorCode errorCode, String message, List<ErrorResponse.FieldError> errors) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .error(errorCode.name())
                .message(message)
                .errors(errors)
                .build();
    }

    static HttpStatus toHttpStatus(ErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case USER_NOT_FOUND, SUBJECT_NOT_FOUND, ASSIGNMENT_NOT_FOUND, DOCUMENT_NOT_FOUND, QUESTION_NOT_FOUND,
                 QUESTION_VERSION_NOT_FOUND, TOPIC_NOT_FOUND, GENERATION_REQUEST_NOT_FOUND,
                 CRITERION_NOT_FOUND, AI_RULE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case UNAUTHORIZED, USER_INACTIVE, FORBIDDEN_SUBJECT, PASSWORD_CHANGE_REQUIRED -> HttpStatus.FORBIDDEN;
            case DRAFT_ALREADY_EXISTS, VERSION_NOT_DRAFT, QUESTION_ARCHIVED, NO_APPROVED_VERSION,
                 CONCURRENT_MODIFICATION, INVALID_DOCUMENT_STATE, DOCUMENT_NOT_RETRYABLE, RETRY_LIMIT_EXCEEDED,
                 DOCUMENT_IN_USE, DOCUMENT_NOT_READY, REGENERATION_NOT_SUPPORTED, SUBJECT_INACTIVE -> HttpStatus.CONFLICT;
            case RUBRIC_SCORE_MISMATCH, SOURCE_REQUIRED, BLOOM_NOT_CONFIRMED, RUBRIC_REQUIRED, CONTENT_REQUIRED,
                 INVALID_CITATION_QUOTE, INSUFFICIENT_CONTEXT, EMPTY_DOCUMENT_TEXT, IMPORT_FILE_INVALID,
                 INVALID_BLOOM_DISTRIBUTION, DOCUMENT_SUBJECT_MISMATCH -> HttpStatus.UNPROCESSABLE_ENTITY;
            case FILE_TOO_LARGE, IMPORT_LIMIT_EXCEEDED -> HttpStatus.PAYLOAD_TOO_LARGE;
            case UNSUPPORTED_FILE_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case AI_SERVICE_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
            case UNCATEGORIZED_EXCEPTION -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
