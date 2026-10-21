package com.example.vieva.application.ports.input.notification;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationView(
        UUID notificationId,
        UUID recipientId,
        UUID questionId,
        UUID versionId,
        String type,
        String message,
        Instant createdAt,
        Instant readAt
) {
}
