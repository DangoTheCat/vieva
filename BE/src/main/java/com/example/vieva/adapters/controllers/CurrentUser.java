package com.example.vieva.adapters.controllers;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.common.ActorView;

import java.util.UUID;

final class CurrentUser {

    private CurrentUser() {
    }

    static UUID id(ActorView actor) {
        if (actor == null || actor.id() == null) {
            throw new ApplicationException(ErrorCode.UNAUTHENTICATED);
        }
        return actor.id();
    }
}
