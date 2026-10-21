package com.example.vieva.application.ports.output;

import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.domain.entities.Notification;
import java.util.UUID;

public interface NotificationRepository {
    void save(Notification notification);
    PageResult<Notification> findByRecipient(UUID recipientId, int page, int size);
    long unreadCount(UUID recipientId);
    boolean markRead(UUID notificationId, UUID recipientId);
}
