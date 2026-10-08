package com.example.vieva.application.ports.output;

/**
 * Output port for outbound email. Implementations: SMTP ({@code vieva.mail.provider=smtp})
 * or a log-only gateway for offline dev/test (the default).
 */
public interface EmailSenderPort {
    void send(String to, String subject, String body);
}
