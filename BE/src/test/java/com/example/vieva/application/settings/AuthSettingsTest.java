package com.example.vieva.application.settings;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSettingsTest {

    @Test
    void onlyUniversityDomainsMayRegister() {
        AuthSettings settings = new AuthSettings();
        assertThat(settings.isRegistrationAllowed("an@fpt.edu.vn")).isTrue();
        assertThat(settings.isRegistrationAllowed("an@fe.fpt.edu.vn")).isTrue();
        assertThat(settings.isRegistrationAllowed("an@gmail.com")).isFalse();
        assertThat(settings.isRegistrationAllowed("an@notfpt.edu.vn")).isFalse();
    }

    @Test
    void emptyListKeepsRegistrationOpen() {
        AuthSettings settings = new AuthSettings();
        settings.setAllowedRegistrationDomains(List.of());
        assertThat(settings.isRegistrationAllowed("an@gmail.com")).isTrue();
    }
}
