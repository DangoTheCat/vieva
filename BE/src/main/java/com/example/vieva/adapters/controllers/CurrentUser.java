package com.example.vieva.adapters.controllers;

import com.example.vieva.domain.entities.User;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;

import java.util.UUID;

final class CurrentUser {

    private CurrentUser() {
    }

    static UUID id(User user) {
        if (user == null || user.getUserId() == null) {
            throw new ApplicationException(ErrorCode.UNAUTHENTICATED);
        }
        return user.getUserId();
    }
}
