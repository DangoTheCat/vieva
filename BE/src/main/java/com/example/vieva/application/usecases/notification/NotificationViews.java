package com.example.vieva.application.usecases.notification;

import com.example.vieva.application.ports.input.notification.NotificationView;
import com.example.vieva.domain.entities.Notification;

public final class NotificationViews {

    private NotificationViews() {
    }

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
