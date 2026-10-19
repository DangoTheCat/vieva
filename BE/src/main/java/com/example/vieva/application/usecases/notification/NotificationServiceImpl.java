package com.example.vieva.application.usecases.notification;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.notification.NotificationService;
import com.example.vieva.application.ports.output.NotificationRepository;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.Notification;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @UseCaseTransaction(readOnly = true)
    public PagedResult<Notification> list(UUID recipientId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        return notificationRepository.findByRecipient(recipientId, page, size);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public long unreadCount(UUID recipientId) {
        return notificationRepository.unreadCount(recipientId);
    }

    @Override
    @UseCaseTransaction
    public void markRead(UUID notificationId, UUID recipientId) {
        if (!notificationRepository.markRead(notificationId, recipientId)) {
            throw new ApplicationException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }
}
