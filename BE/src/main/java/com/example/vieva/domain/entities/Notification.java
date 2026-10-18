package com.example.vieva.domain.entities;

import java.time.Instant;
import java.util.UUID;

public record Notification(UUID notificationId, UUID recipientId, UUID questionId, UUID versionId,
                           String type, String message, Instant createdAt, Instant readAt) { }
