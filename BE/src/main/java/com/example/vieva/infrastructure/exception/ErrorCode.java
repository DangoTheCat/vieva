package com.example.vieva.infrastructure.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION("9999", "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND("1001", "User not found", HttpStatus.NOT_FOUND),
    USER_EXISTED("1002", "User already exists", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED("1003", "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("1004", "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_KEY("1005", "Invalid message key", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("1006", "Invalid request parameters", HttpStatus.BAD_REQUEST),
    USER_INACTIVE("1007", "User account is not active", HttpStatus.FORBIDDEN),
    ROLE_NOT_FOUND("1008", "Default role not found", HttpStatus.INTERNAL_SERVER_ERROR),
    INCORRECT_PASSWORD("1009", "Incorrect old password", HttpStatus.BAD_REQUEST),
    PASSWORD_UNCHANGED("1010", "New password must be different from old password", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
