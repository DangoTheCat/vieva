package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.notification.NotificationService;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.domain.entities.Notification;
import com.example.vieva.domain.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;

    @GetMapping
    public PagedResult<Notification> list(@AuthenticationPrincipal User user,
                                          @RequestParam(defaultValue="0") int page,
                                          @RequestParam(defaultValue="20") int size) {
        return notifications.list(CurrentUser.id(user), page, size);
    }

    @GetMapping("/unread-count")
    public Map<String,Long> unread(@AuthenticationPrincipal User user) {
        return Map.of("count", notifications.unreadCount(CurrentUser.id(user)));
    }

    @PatchMapping("/{id}/read")
    public void read(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        notifications.markRead(id, CurrentUser.id(user));
    }
}
