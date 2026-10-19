package com.example.vieva.application.usecases.notification;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.output.NotificationRepository;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.domain.entities.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationServiceImpl notificationService;

    private final UUID recipientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository);
    }

    @Test
    @DisplayName("list rejects negative page, zero size, or size over 100")
    void listValidatesPagination() {
        assertThatThrownBy(() -> notificationService.list(recipientId, -1, 10))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);

        assertThatThrownBy(() -> notificationService.list(recipientId, 0, 0))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);

        assertThatThrownBy(() -> notificationService.list(recipientId, 0, 101))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("list delegates to repository when pagination is valid")
    void listDelegatesToRepository() {
        PagedResult<Notification> expected = PagedResult.of(List.of(), 0, 20, 0);
        when(notificationRepository.findByRecipient(recipientId, 0, 20)).thenReturn(expected);

        PagedResult<Notification> actual = notificationService.list(recipientId, 0, 20);

        assertThat(actual).isSameAs(expected);
        verify(notificationRepository).findByRecipient(recipientId, 0, 20);
    }

    @Test
    @DisplayName("unreadCount delegates to repository")
    void unreadCountDelegates() {
        when(notificationRepository.unreadCount(recipientId)).thenReturn(5L);

        long count = notificationService.unreadCount(recipientId);

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository).unreadCount(recipientId);
    }

    @Test
    @DisplayName("markRead throws NOTIFICATION_NOT_FOUND when repository returns false")
    void markReadNotFound() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.markRead(notificationId, recipientId)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.markRead(notificationId, recipientId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("markRead succeeds when repository returns true")
    void markReadSuccess() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.markRead(notificationId, recipientId)).thenReturn(true);

        notificationService.markRead(notificationId, recipientId);

        verify(notificationRepository).markRead(notificationId, recipientId);
    }
}
