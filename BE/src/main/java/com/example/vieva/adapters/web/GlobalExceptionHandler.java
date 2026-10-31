package com.example.vieva.adapters.web;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCategory;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.common.ErrorTranslator;
import com.example.vieva.application.ports.input.common.ErrorView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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

    private final ErrorTranslator errorTranslator;

    public GlobalExceptionHandler(ErrorTranslator errorTranslator) {
        this.errorTranslator = errorTranslator;
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException exception) {
        ErrorView view = errorTranslator.translate(exception);
        log.warn("Application exception [{} {}]: {}", view.code(), view.error(), view.message());
        return toResponseEntity(view);
    }

    public ResponseEntity<ErrorResponse> handleAppException(ApplicationException exception) {
        return handleApplicationException(exception);
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
                || (msg.contains("email") && msg.contains("unique")))) {
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

    // Unknown path (e.g. a trailing slash) or wrong HTTP method: a client error, not a 500
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(ErrorCode.INVALID_REQUEST, "Endpoint not found: /" + exception.getResourcePath(), null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(body(ErrorCode.INVALID_REQUEST, exception.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        ErrorView view = errorTranslator.translate(exception);
        if (view != null && view.category() != ErrorCategory.INTERNAL) {
            log.warn("Translated exception [{} {}]: {}", view.code(), view.error(), view.message());
            return toResponseEntity(view);
        }
        // Full stack trace goes to the log only — never to the client.
        log.error("Unhandled exception: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(ErrorCode.UNCATEGORIZED_EXCEPTION, ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage(), null));
    }

    private ResponseEntity<ErrorResponse> toResponseEntity(ErrorView view) {
        List<ErrorResponse.FieldError> fieldErrors = (view.errors() == null || view.errors().isEmpty()) ? null
                : view.errors().stream()
                .map(v -> ErrorResponse.FieldError.builder()
                        .field(v.field())
                        .error(v.error())
                        .message(v.message())
                        .build())
                .toList();

        HttpStatus status = toHttpStatus(view.category());
        ErrorResponse responseBody = ErrorResponse.builder()
                .code(view.code())
                .error(view.error())
                .message(view.message())
                .errors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(responseBody);
    }

    private static ErrorResponse body(ErrorCode errorCode, String message, List<ErrorResponse.FieldError> errors) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .error(errorCode.name())
                .message(message)
                .errors(errors)
                .build();
    }

    static HttpStatus toHttpStatus(ErrorCategory category) {
        if (category == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (category) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNPROCESSABLE_ENTITY -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PAYLOAD_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case BAD_GATEWAY -> HttpStatus.BAD_GATEWAY;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
            case VALIDATION, BAD_REQUEST -> HttpStatus.BAD_REQUEST;
        };
    }
}
