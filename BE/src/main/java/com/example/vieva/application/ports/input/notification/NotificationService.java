package com.example.vieva.application.ports.input.notification;

import com.example.vieva.application.ports.output.PagedResult;

import java.util.UUID;

public interface NotificationService {
    PagedResult<NotificationView> list(UUID recipientId, int page, int size);
    long unreadCount(UUID recipientId);
    void markRead(UUID notificationId, UUID recipientId);
}
