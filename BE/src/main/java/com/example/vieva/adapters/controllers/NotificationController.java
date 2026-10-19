package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.output.*;
import com.example.vieva.domain.entities.*;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.exception.FieldViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notifications;

    @GetMapping
    @Transactional(readOnly = true)
    public PagedResult<Notification> list(@AuthenticationPrincipal User user,
                                          @RequestParam(defaultValue="0") int page,
                                          @RequestParam(defaultValue="20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        return notifications.findByRecipient(CurrentUser.id(user),page,size);
    }

    @GetMapping("/unread-count")
    public Map<String,Long> unread(@AuthenticationPrincipal User user) {
        return Map.of("count",notifications.unreadCount(CurrentUser.id(user)));
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public void read(@PathVariable UUID id,@AuthenticationPrincipal User user) {
        if (!notifications.markRead(id,CurrentUser.id(user))) throw new ApplicationException(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
