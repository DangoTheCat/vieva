package com.example.vieva.application.usecases.common;

import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.common.ErrorTranslator;
import com.example.vieva.application.ports.input.common.ErrorView;
import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class DomainErrorCodeMappingTest {

    private final ErrorTranslator translator = new ErrorTranslatorImpl();

    @ParameterizedTest
    @EnumSource(DomainErrorCode.class)
    @DisplayName("Every DomainErrorCode must have an identical ErrorCode counterpart with same name, code and message")
    void domainErrorCodeMatchesApplicationErrorCode(DomainErrorCode domainCode) {
        ErrorCode errorCode = null;
        try {
            errorCode = ErrorCode.valueOf(domainCode.name());
        } catch (IllegalArgumentException e) {
            org.junit.jupiter.api.Assertions.fail(
                    "ErrorCode counterpart missing for DomainErrorCode." + domainCode.name()
            );
        }

        assertThat(errorCode).isNotNull();
        assertThat(errorCode.getCode())
                .as("Code mismatch for %s", domainCode.name())
                .isEqualTo(domainCode.getCode());
        assertThat(errorCode.getMessage())
                .as("Message mismatch for %s", domainCode.name())
                .isEqualTo(domainCode.getMessage());
    }

    @ParameterizedTest
    @EnumSource(DomainErrorCode.class)
    @DisplayName("ErrorTranslator translates every DomainErrorCode without falling back to UNCATEGORIZED_EXCEPTION")
    void errorTranslatorTranslatesDomainException(DomainErrorCode domainCode) {
        ErrorView view = translator.translate(new DomainException(domainCode));

        assertThat(view).isNotNull();
        assertThat(view.code()).isEqualTo(domainCode.getCode());
        assertThat(view.error()).isEqualTo(domainCode.name());
        assertThat(view.message()).isEqualTo(domainCode.getMessage());
        assertThat(view.code()).isNotEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
    }
}
