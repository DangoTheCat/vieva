package com.example.vieva.application.ports.input.notification;

import com.example.vieva.domain.entities.Notification;
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
    public static NotificationView from(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationView(
                notification.notificationId(),
                notification.recipientId(),
                notification.questionId(),
                notification.versionId(),
                notification.type(),
                notification.message(),
                notification.createdAt(),
                notification.readAt()
        );
    }
}
