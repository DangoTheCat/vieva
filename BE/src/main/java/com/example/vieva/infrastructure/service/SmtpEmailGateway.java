package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends email through the SMTP server configured by {@code spring.mail.*}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vieva.mail.provider", havingValue = "smtp")
public class SmtpEmailGateway implements EmailSenderPort {

    private final JavaMailSender mailSender;

    @Value("${vieva.mail.from:no-reply@vieva.local}")
    private String from;

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
