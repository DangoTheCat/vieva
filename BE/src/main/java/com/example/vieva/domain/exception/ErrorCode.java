package com.example.vieva.domain.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION("9999", "Uncategorized error"),
    USER_NOT_FOUND("1001", "User not found"),
    USER_EXISTED("1002", "User already exists"),
    UNAUTHENTICATED("1003", "Unauthenticated"),
    UNAUTHORIZED("1004", "You do not have permission"),
    INVALID_KEY("1005", "Invalid message key"),
    INVALID_REQUEST("1006", "Invalid request parameters"),
    USER_INACTIVE("1007", "User account is not active"),
    ROLE_NOT_FOUND("1008", "Role not found"),
    INCORRECT_PASSWORD("1009", "Incorrect old password"),
    PASSWORD_UNCHANGED("1010", "New password must be different from old password"),
    CANNOT_DELETE_SELF("1011", "You cannot delete your own account"),
    CANNOT_DEMOTE_SELF("1012", "You cannot demote your own admin role"),
    CANNOT_DELETE_LAST_ADMIN("1013", "Cannot delete the last administrator in the system"),
    CANNOT_DEMOTE_LAST_ADMIN("1014", "Cannot demote the last administrator in the system"),
    USER_CODE_EXISTED("1015", "User code already exists"),
    CANNOT_LOCK_SELF("1016", "You cannot lock or deactivate your own account"),
    CANNOT_LOCK_LAST_ADMIN("1017", "Cannot lock or deactivate the last active administrator"),
    SUBJECT_NOT_FOUND("1018", "Subject not found"),
    SUBJECT_CODE_EXISTED("1019", "Subject code already exists"),
    NOT_A_LECTURER("1020", "User does not have LECTURER role"),
    CANNOT_ASSIGN_INACTIVE_USER("1021", "Cannot assign inactive user"),
    CANNOT_ASSIGN_INACTIVE_SUBJECT("1022", "Cannot assign to inactive subject"),
    CANNOT_ASSIGN_DUPLICATE("1023", "Lecturer is already actively assigned to this subject"),
    ASSIGNMENT_NOT_FOUND("1024", "Lecturer subject assignment not found"),
    DOCUMENT_NOT_FOUND("1025", "Course document not found"),
    DOCUMENT_NOT_READY("1026", "Course document is not READY; only READY documents can be used for retrieval"),
    EMPTY_DOCUMENT_TEXT("1027", "No text could be extracted from the document (empty or scanned without a text layer)"),
    INSUFFICIENT_CONTEXT("1028", "No relevant context found in the selected course documents"),
    QUESTION_NOT_FOUND("1029", "Question not found"),
    QUESTION_VERSION_NOT_FOUND("1030", "Question version not found"),
    DRAFT_ALREADY_EXISTS("1031", "A draft version already exists for this question"),
    VERSION_NOT_DRAFT("1032", "Only DRAFT versions can be modified, approved, rejected or deleted"),
    INVALID_CITATION_QUOTE("1033", "Citation quote does not match the source document chunk"),
    RUBRIC_SCORE_MISMATCH("1034", "Sum of criteria max scores must equal the rubric total score"),
    FORBIDDEN_SUBJECT("1035", "You are not assigned to manage this subject"),
    TOPIC_NOT_FOUND("1036", "Topic not found"),
    TOPIC_NOT_IN_SUBJECT("1037", "Topic does not belong to the specified subject"),
    SOURCE_REQUIRED("1038", "AI-generated questions need at least one valid source chunk before approval"),
    BLOOM_NOT_CONFIRMED("1039", "The Bloom level suggested by AI must be confirmed by the lecturer before approval"),
    RUBRIC_REQUIRED("1040", "A rubric with at least one criterion is required"),
    CONTENT_REQUIRED("1041", "Question content and expected answer are required"),
    QUESTION_ARCHIVED("1042", "Question is archived"),
    NO_APPROVED_VERSION("1043", "Question has no APPROVED version"),
    CONCURRENT_MODIFICATION("1044", "Resource was modified concurrently, reload and retry"),
    UNSUPPORTED_FILE_TYPE("1045", "File type is not supported"),
    FILE_TOO_LARGE("1046", "File exceeds the maximum allowed size"),
    INVALID_DOCUMENT_STATE("1047", "Invalid document state transition"),
    DOCUMENT_NOT_RETRYABLE("1048", "Only FAILED documents can be re-indexed"),
    RETRY_LIMIT_EXCEEDED("1049", "Retry limit exceeded"),
    DOCUMENT_IN_USE("1050", "Document is referenced by question sources and cannot be deleted"),
    DOCUMENT_SUBJECT_MISMATCH("1051", "Selected documents must belong to the subject"),
    INVALID_BLOOM_DISTRIBUTION("1052", "Invalid Bloom distribution"),
    GENERATION_REQUEST_NOT_FOUND("1053", "Question generation request not found"),
    AI_SERVICE_UNAVAILABLE("1054", "AI service failed, please retry later"),
    REGENERATION_NOT_SUPPORTED("1055", "Only AI-generated DRAFT versions with a stored context can be regenerated"),
    CRITERION_NOT_FOUND("1056", "Rubric criterion not found"),
    IMPORT_FILE_INVALID("1057", "Import file is invalid"),
    IMPORT_LIMIT_EXCEEDED("1058", "Import file exceeds the configured limits"),
    SUBJECT_INACTIVE("1059", "Subject is not active"),
    AI_RULE_NOT_FOUND("1060", "Active AI rule not found");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
