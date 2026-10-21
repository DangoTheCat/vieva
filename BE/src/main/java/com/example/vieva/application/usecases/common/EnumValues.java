package com.example.vieva.application.usecases.common;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;

public final class EnumValues {

    private EnumValues() {
    }

    public static <E extends Enum<E>> E parse(Class<E> enumClass, String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim());
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, errorMessage);
        }
    }
}
