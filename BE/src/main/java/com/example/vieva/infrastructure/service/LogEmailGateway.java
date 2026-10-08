package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.EmailSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Offline email for dev/test ({@code vieva.mail.provider=log}, the default): logs recipient and subject only.
 * The body is never logged because it can carry a temporary password.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "vieva.mail.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailGateway implements EmailSenderPort {

    public LogEmailGateway() {
        log.warn("Log-only email provider active — set VIEVA_MAIL_PROVIDER=smtp to send real email");
    }

    @Override
    public void send(String to, String subject, String body) {
        log.info("Email not sent (log provider): to={}, subject={}", to, subject);
    }
}
