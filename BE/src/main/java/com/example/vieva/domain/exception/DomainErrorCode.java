package com.example.vieva.domain.exception;

import lombok.Getter;

@Getter
public enum DomainErrorCode {
    INVALID_REQUEST("1006", "Invalid request parameters"),
    INVALID_CITATION_QUOTE("1033", "Citation quote does not match the source document chunk"),
    RUBRIC_SCORE_MISMATCH("1034", "Sum of criteria max scores must equal the rubric total score"),
    SOURCE_REQUIRED("1038", "AI-generated questions need at least one valid source chunk before approval"),
    BLOOM_NOT_CONFIRMED("1039", "The Bloom level suggested by AI must be confirmed by the lecturer before approval"),
    RUBRIC_REQUIRED("1040", "A rubric with at least one criterion is required"),
    CONTENT_REQUIRED("1041", "Question content and expected answer are required"),
    QUESTION_ARCHIVED("1042", "Question is archived"),
    NO_APPROVED_VERSION("1043", "Question has no APPROVED version"),
    CONCURRENT_MODIFICATION("1044", "Resource was modified concurrently, reload and retry"),
    INVALID_DOCUMENT_STATE("1047", "Invalid document state transition"),
    DOCUMENT_NOT_RETRYABLE("1048", "Only FAILED documents can be re-indexed"),
    RETRY_LIMIT_EXCEEDED("1049", "Retry limit exceeded"),
    INVALID_BLOOM_DISTRIBUTION("1052", "Invalid Bloom distribution"),
    REGENERATION_NOT_SUPPORTED("1055", "Only AI-generated DRAFT versions with a stored context can be regenerated"),
    VERSION_NOT_UNDER_REVIEW("1063", "Only UNDER_REVIEW versions can be approved or rejected"),
    VERSION_NOT_DRAFT("1032", "Only DRAFT versions can be modified, approved, rejected or deleted");

    private final String code;
    private final String message;

    DomainErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
