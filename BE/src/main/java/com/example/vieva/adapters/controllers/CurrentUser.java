package com.example.vieva.adapters.controllers;

import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;

import java.util.UUID;

final class CurrentUser {

    private CurrentUser() {
    }

    static UUID id(User user) {
        if (user == null || user.getUserId() == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return user.getUserId();
    }
}
