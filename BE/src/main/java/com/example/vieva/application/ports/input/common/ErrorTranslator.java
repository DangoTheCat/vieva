package com.example.vieva.application.ports.input.common;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.domain.exception.DomainException;

public interface ErrorTranslator {
    ErrorView translate(ApplicationException exception);
    ErrorView translate(DomainException exception);
    ErrorView translate(Throwable throwable);
}
