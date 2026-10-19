package com.example.vieva.application.usecases.common;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCategory;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.common.ErrorTranslator;
import com.example.vieva.application.ports.input.common.ErrorView;
import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;

import java.util.List;

public class ErrorTranslatorImpl implements ErrorTranslator {

    @Override
    public ErrorView translate(ApplicationException exception) {
        if (exception instanceof DuplicateKeyException dke) {
            return translateDuplicateKey(dke);
        }
        ErrorCode code = exception.getErrorCode() != null ? exception.getErrorCode() : ErrorCode.UNCATEGORIZED_EXCEPTION;
        ErrorCategory category = categoryOf(code);
        String message = exception.getMessage() != null ? exception.getMessage() : code.getMessage();
        List<ErrorView.FieldError> errors = (exception.getViolations() == null || exception.getViolations().isEmpty())
                ? null
                : exception.getViolations().stream()
                .map(v -> new ErrorView.FieldError(v.field(), v.code() != null ? v.code().name() : null, v.message()))
                .toList();
        return new ErrorView(code.getCode(), code.name(), message, category, errors);
    }

    @Override
    public ErrorView translate(DomainException exception) {
        DomainErrorCode dCode = exception.getErrorCode();
        ErrorCode code = ErrorCode.UNCATEGORIZED_EXCEPTION;
        if (dCode != null) {
            try {
                code = ErrorCode.valueOf(dCode.name());
            } catch (IllegalArgumentException ignored) {
                // fallback to uncategorized if domain code is not mapped in application
            }
        }
        ErrorCategory category = categoryOf(code);
        String message = exception.getMessage() != null
                ? exception.getMessage()
                : (dCode != null ? dCode.getMessage() : code.getMessage());
        List<ErrorView.FieldError> errors = (exception.getViolations() == null || exception.getViolations().isEmpty())
                ? null
                : exception.getViolations().stream()
                .map(v -> new ErrorView.FieldError(v.field(), v.code() != null ? v.code().name() : null, v.message()))
                .toList();
        return new ErrorView(code.getCode(), code.name(), message, category, errors);
    }

    @Override
    public ErrorView translate(Throwable throwable) {
        if (throwable instanceof ApplicationException ae) {
            return translate(ae);
        }
        if (throwable instanceof DomainException de) {
            return translate(de);
        }
        return new ErrorView(
                ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                ErrorCode.UNCATEGORIZED_EXCEPTION.name(),
                ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage(),
                ErrorCategory.INTERNAL,
                null
        );
    }

    private ErrorView translateDuplicateKey(DuplicateKeyException exception) {
        ErrorCode code = exception.getSuggestedErrorCode();
        if (code == null) {
            StringBuilder sb = new StringBuilder();
            Throwable current = exception;
            while (current != null) {
                if (current.getMessage() != null) {
                    sb.append(current.getMessage()).append(" ");
                }
                current = current.getCause();
            }
            String msg = sb.toString();
            if (msg.contains("uq_users_active_email") || msg.contains("uq_users_active_user_code")
                    || (msg.contains("email") && msg.contains("unique"))) {
                code = ErrorCode.USER_EXISTED;
            } else if (msg.contains("uq_question_draft_per_question")) {
                code = ErrorCode.DRAFT_ALREADY_EXISTS;
            } else {
                code = ErrorCode.INVALID_REQUEST;
            }
        }

        ErrorCategory category = categoryOf(code);
        String message;
        if (code == ErrorCode.USER_EXISTED) {
            category = ErrorCategory.BAD_REQUEST;
            message = ErrorCode.USER_EXISTED.getMessage();
        } else if (code == ErrorCode.DRAFT_ALREADY_EXISTS) {
            category = ErrorCategory.CONFLICT;
            message = ErrorCode.DRAFT_ALREADY_EXISTS.getMessage();
        } else if (code == ErrorCode.INVALID_REQUEST) {
            category = ErrorCategory.CONFLICT;
            message = "Request conflicts with an existing resource";
        } else {
            message = exception.getMessage() != null ? exception.getMessage() : code.getMessage();
        }

        return new ErrorView(code.getCode(), code.name(), message, category, null);
    }

    public static ErrorCategory categoryOf(ErrorCode errorCode) {
        if (errorCode == null) {
            return ErrorCategory.INTERNAL;
        }
        return switch (errorCode) {
            case USER_NOT_FOUND, SUBJECT_NOT_FOUND, ASSIGNMENT_NOT_FOUND, DOCUMENT_NOT_FOUND, QUESTION_NOT_FOUND,
                 QUESTION_VERSION_NOT_FOUND, TOPIC_NOT_FOUND, GENERATION_REQUEST_NOT_FOUND,
                 CRITERION_NOT_FOUND, AI_RULE_NOT_FOUND, NOTIFICATION_NOT_FOUND -> ErrorCategory.NOT_FOUND;
            case UNAUTHENTICATED -> ErrorCategory.UNAUTHENTICATED;
            case UNAUTHORIZED, USER_INACTIVE, FORBIDDEN_SUBJECT, PASSWORD_CHANGE_REQUIRED, NOT_SUBJECT_HEAD,
                 NOT_QUESTION_AUTHOR -> ErrorCategory.FORBIDDEN;
            case VERSION_NOT_UNDER_REVIEW, SUBJECT_HEAD_REQUIRED, DRAFT_ALREADY_EXISTS, VERSION_NOT_DRAFT,
                 QUESTION_ARCHIVED, NO_APPROVED_VERSION, CONCURRENT_MODIFICATION, INVALID_DOCUMENT_STATE,
                 DOCUMENT_NOT_RETRYABLE, RETRY_LIMIT_EXCEEDED, DOCUMENT_IN_USE, DOCUMENT_NOT_READY,
                 REGENERATION_NOT_SUPPORTED, SUBJECT_INACTIVE -> ErrorCategory.CONFLICT;
            case RUBRIC_SCORE_MISMATCH, SOURCE_REQUIRED, BLOOM_NOT_CONFIRMED, RUBRIC_REQUIRED, CONTENT_REQUIRED,
                 INVALID_CITATION_QUOTE, INSUFFICIENT_CONTEXT, EMPTY_DOCUMENT_TEXT, IMPORT_FILE_INVALID,
                 INVALID_BLOOM_DISTRIBUTION, DOCUMENT_SUBJECT_MISMATCH -> ErrorCategory.UNPROCESSABLE_ENTITY;
            case FILE_TOO_LARGE, IMPORT_LIMIT_EXCEEDED -> ErrorCategory.PAYLOAD_TOO_LARGE;
            case UNSUPPORTED_FILE_TYPE -> ErrorCategory.UNSUPPORTED_MEDIA_TYPE;
            case AI_SERVICE_UNAVAILABLE -> ErrorCategory.BAD_GATEWAY;
            case UNCATEGORIZED_EXCEPTION -> ErrorCategory.INTERNAL;
            default -> ErrorCategory.BAD_REQUEST;
        };
    }
}
