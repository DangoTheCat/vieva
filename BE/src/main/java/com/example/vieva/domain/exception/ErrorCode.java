package com.example.vieva.domain.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION("9999", "Uncategorized error", 500),
    USER_NOT_FOUND("1001", "User not found", 404),
    USER_EXISTED("1002", "User already exists", 400),
    UNAUTHENTICATED("1003", "Unauthenticated", 401),
    UNAUTHORIZED("1004", "You do not have permission", 403),
    INVALID_KEY("1005", "Invalid message key", 400),
    INVALID_REQUEST("1006", "Invalid request parameters", 400),
    USER_INACTIVE("1007", "User account is not active", 403),
    ROLE_NOT_FOUND("1008", "Default role not found", 500),
    INCORRECT_PASSWORD("1009", "Incorrect old password", 400),
    PASSWORD_UNCHANGED("1010", "New password must be different from old password", 400);

    private final String code;
    private final String message;
    private final int httpStatusCode;

    ErrorCode(String code, String message, int httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
