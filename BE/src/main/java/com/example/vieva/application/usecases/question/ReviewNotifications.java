package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.output.NotificationRepository;
import com.example.vieva.domain.entities.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

/** Inserts join the review transaction: rollback also removes its notification. */
@Component
@RequiredArgsConstructor
public class ReviewNotifications {
    private final NotificationRepository notifications;

    public void notifyPendingReview(UUID recipient, UUID questionId, UUID versionId) {
        notifications.save(new Notification(UUID.randomUUID(),recipient,questionId,versionId,"PENDING_REVIEW",
                "Có câu hỏi đang chờ trưởng bộ môn phê duyệt",Instant.now(),null));
    }

    public void notifyQuestionApproved(UUID recipient, UUID questionId, UUID versionId) {
        notifications.save(new Notification(UUID.randomUUID(),recipient,questionId,versionId,"QUESTION_APPROVED",
                "Câu hỏi của bạn đã được phê duyệt",Instant.now(),null));
    }
}
