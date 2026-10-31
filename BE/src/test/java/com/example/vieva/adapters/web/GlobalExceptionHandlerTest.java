package com.example.vieva.adapters.web;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.application.usecases.common.ErrorTranslatorImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ErrorTranslatorImpl());

    private static final Map<ErrorCode, HttpStatus> EXPECTED_STATUSES = Map.ofEntries(
            // 404 NOT_FOUND
            Map.entry(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.SUBJECT_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.ASSIGNMENT_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.DOCUMENT_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.QUESTION_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.QUESTION_VERSION_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.TOPIC_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.GENERATION_REQUEST_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.CRITERION_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.AI_RULE_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.NOTIFICATION_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.RUBRIC_TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.EXAM_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.ATTEMPT_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.TURN_NOT_FOUND, HttpStatus.NOT_FOUND),

            // 401 UNAUTHORIZED
            Map.entry(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED),

            // 403 FORBIDDEN
            Map.entry(ErrorCode.UNAUTHORIZED, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.USER_INACTIVE, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.FORBIDDEN_SUBJECT, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.PASSWORD_CHANGE_REQUIRED, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.NOT_SUBJECT_HEAD, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.NOT_QUESTION_AUTHOR, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.ATTEMPT_NOT_OWNED, HttpStatus.FORBIDDEN),
            Map.entry(ErrorCode.GRADING_FORBIDDEN, HttpStatus.FORBIDDEN),

            // 409 CONFLICT
            Map.entry(ErrorCode.VERSION_NOT_UNDER_REVIEW, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.SUBJECT_HEAD_REQUIRED, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.DRAFT_ALREADY_EXISTS, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.VERSION_NOT_DRAFT, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.QUESTION_ARCHIVED, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.NO_APPROVED_VERSION, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.CONCURRENT_MODIFICATION, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.INVALID_DOCUMENT_STATE, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.DOCUMENT_NOT_RETRYABLE, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.RETRY_LIMIT_EXCEEDED, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.DOCUMENT_IN_USE, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.DOCUMENT_NOT_READY, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.REGENERATION_NOT_SUPPORTED, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.SUBJECT_INACTIVE, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.CANNOT_DELETE_STUDENT_WITH_ATTEMPTS, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.EXAM_NOT_DRAFT, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.EXAM_CLOSED, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.EXAM_NOT_ACTIVE, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.STUDENT_ALREADY_ATTEMPTED, HttpStatus.CONFLICT),

            // 422 UNPROCESSABLE_ENTITY
            Map.entry(ErrorCode.RUBRIC_SCORE_MISMATCH, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.SOURCE_REQUIRED, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.BLOOM_NOT_CONFIRMED, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.RUBRIC_REQUIRED, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.CONTENT_REQUIRED, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.INVALID_CITATION_QUOTE, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.INSUFFICIENT_CONTEXT, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.EMPTY_DOCUMENT_TEXT, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.IMPORT_FILE_INVALID, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.INVALID_BLOOM_DISTRIBUTION, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.DOCUMENT_SUBJECT_MISMATCH, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.EXAM_QUESTION_MISMATCH, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(ErrorCode.MAX_FOLLOWUPS_EXCEEDED, HttpStatus.UNPROCESSABLE_ENTITY),

            // 413 PAYLOAD_TOO_LARGE
            Map.entry(ErrorCode.FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE),
            Map.entry(ErrorCode.IMPORT_LIMIT_EXCEEDED, HttpStatus.PAYLOAD_TOO_LARGE),

            // 415 UNSUPPORTED_MEDIA_TYPE
            Map.entry(ErrorCode.UNSUPPORTED_FILE_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE),

            // 502 BAD_GATEWAY
            Map.entry(ErrorCode.AI_SERVICE_UNAVAILABLE, HttpStatus.BAD_GATEWAY),

            // 500 INTERNAL_SERVER_ERROR
            Map.entry(ErrorCode.UNCATEGORIZED_EXCEPTION, HttpStatus.INTERNAL_SERVER_ERROR),

            // 400 BAD_REQUEST (default)
            Map.entry(ErrorCode.USER_EXISTED, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.INVALID_KEY, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.ROLE_NOT_FOUND, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.INCORRECT_PASSWORD, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.PASSWORD_UNCHANGED, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_DELETE_SELF, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_DEMOTE_SELF, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_DELETE_LAST_ADMIN, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_DEMOTE_LAST_ADMIN, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.USER_CODE_EXISTED, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_LOCK_SELF, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_LOCK_LAST_ADMIN, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.SUBJECT_CODE_EXISTED, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.NOT_A_LECTURER, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_ASSIGN_INACTIVE_USER, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_ASSIGN_INACTIVE_SUBJECT, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.CANNOT_ASSIGN_DUPLICATE, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.TOPIC_NOT_IN_SUBJECT, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.MULTIPLE_ROLES_NOT_ALLOWED, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.DOCUMENT_ALREADY_EXISTS, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.INVALID_SPEECH_VOICE, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.INVALID_SPEECH_LOCALE, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.INVALID_SCORE, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.REGISTRATION_DOMAIN_NOT_ALLOWED, HttpStatus.FORBIDDEN)
    );

    @Test
    @DisplayName("Every ErrorCode must be mapped in the expected statuses table")
    void allErrorCodeValuesAreCovered() {
        assertThat(EXPECTED_STATUSES.keySet()).containsExactlyInAnyOrder(ErrorCode.values());
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("GlobalExceptionHandler must return expected HTTP status and code for each ErrorCode")
    void allErrorsReturnExpectedHttpStatus(ErrorCode errorCode) {
        ResponseEntity<ErrorResponse> response = handler.handleApplicationException(new ApplicationException(errorCode));
        HttpStatus expectedStatus = EXPECTED_STATUSES.get(errorCode);

        assertThat(response.getStatusCode())
                .as("Status code for ErrorCode.%s", errorCode.name())
                .isEqualTo(expectedStatus);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(errorCode.getCode());
        assertThat(response.getBody().getError()).isEqualTo(errorCode.name());
    }

    @Test
    @DisplayName("DuplicateKeyException with email/user_code constraint returns 400 USER_EXISTED")
    void duplicateKeyExceptionActiveEmailOrUserCodeReturns400() {
        DuplicateKeyException emailEx = new DuplicateKeyException(
                "User duplicate",
                new DataIntegrityViolationException("duplicate", new RuntimeException("violates unique constraint \"uq_users_active_email\""))
        );
        ResponseEntity<ErrorResponse> res1 = handler.handleApplicationException(emailEx);
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res1.getBody()).isNotNull();
        assertThat(res1.getBody().getCode()).isEqualTo(ErrorCode.USER_EXISTED.getCode());

        DuplicateKeyException userCodeEx = new DuplicateKeyException(
                "User duplicate",
                new DataIntegrityViolationException("duplicate", new RuntimeException("violates unique constraint \"uq_users_active_user_code\""))
        );
        ResponseEntity<ErrorResponse> res2 = handler.handleApplicationException(userCodeEx);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res2.getBody()).isNotNull();
        assertThat(res2.getBody().getCode()).isEqualTo(ErrorCode.USER_EXISTED.getCode());

        DuplicateKeyException genericEmailEx = new DuplicateKeyException(
                "User duplicate",
                new DataIntegrityViolationException("duplicate", new RuntimeException("email unique constraint"))
        );
        ResponseEntity<ErrorResponse> res3 = handler.handleApplicationException(genericEmailEx);
        assertThat(res3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res3.getBody()).isNotNull();
        assertThat(res3.getBody().getCode()).isEqualTo(ErrorCode.USER_EXISTED.getCode());
    }

    @Test
    @DisplayName("DuplicateKeyException with question draft constraint returns 409 DRAFT_ALREADY_EXISTS")
    void duplicateKeyExceptionDraftConstraintReturns409() {
        DuplicateKeyException draftEx = new DuplicateKeyException(
                "QuestionVersion duplicate",
                new DataIntegrityViolationException("duplicate", new RuntimeException("violates unique constraint \"uq_question_draft_per_question\""))
        );
        ResponseEntity<ErrorResponse> res = handler.handleApplicationException(draftEx);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.DRAFT_ALREADY_EXISTS.getCode());
    }

    @Test
    @DisplayName("DuplicateKeyException with other constraint returns 409 INVALID_REQUEST")
    void duplicateKeyExceptionOtherConstraintReturns409() {
        DuplicateKeyException otherEx = new DuplicateKeyException(
                "Other duplicate",
                new DataIntegrityViolationException("duplicate", new RuntimeException("violates unique constraint \"uq_subject_code\""))
        );
        ResponseEntity<ErrorResponse> res = handler.handleApplicationException(otherEx);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.INVALID_REQUEST.getCode());
    }
}
