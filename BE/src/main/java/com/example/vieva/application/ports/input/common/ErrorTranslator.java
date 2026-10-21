package com.example.vieva.application.ports.input.common;

public interface ErrorTranslator {
    ErrorView translate(Throwable throwable);
}
