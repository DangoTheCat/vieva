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
    CANNOT_LOCK_LAST_ADMIN("1017", "Cannot lock or deactivate the last active administrator");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
