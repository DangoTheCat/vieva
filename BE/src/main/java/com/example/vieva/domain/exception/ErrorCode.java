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
    DOCUMENT_NOT_READY("1026", "Course document is not ready for retrieval or indexing"),
    EMPTY_DOCUMENT_TEXT("1027", "Extracted text is empty or scanned document without text layer"),
    INSUFFICIENT_CONTEXT("1028", "No relevant context found in course documents for this request"),
    QUESTION_NOT_FOUND("1029", "Question not found"),
    QUESTION_VERSION_NOT_FOUND("1030", "Question version not found"),
    DRAFT_ALREADY_EXISTS("1031", "A draft version already exists for this question"),
    CANNOT_MODIFY_NON_DRAFT("1032", "Only DRAFT versions can be modified or deleted"),
    INVALID_CITATION_QUOTE("1033", "Citation quote does not match source document chunk content"),
    INVALID_RUBRIC_TOTAL("1034", "Sum of criteria max points must equal rubric total points"),
    UNAUTHORIZED_SUBJECT_ACCESS("1035", "You are not assigned to manage this subject"),
    TOPIC_NOT_FOUND("1036", "Topic not found"),
    TOPIC_NOT_IN_SUBJECT("1037", "Topic does not belong to the specified subject");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
