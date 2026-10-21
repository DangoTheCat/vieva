package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.output.NotificationRepository;
import com.example.vieva.domain.entities.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {
    private final JdbcTemplate jdbc;

    public void save(Notification n) {
        jdbc.update("INSERT INTO notifications(notification_id,recipient_id,question_id,version_id,type,message,created_at) VALUES (?,?,?,?,?,?,?)",
                n.notificationId(), n.recipientId(), n.questionId(), n.versionId(), n.type(), n.message(),
                java.sql.Timestamp.from(n.createdAt()));
    }

    public PageResult<Notification> findByRecipient(UUID recipient, int page, int size) {
        var rows = jdbc.query("SELECT * FROM notifications WHERE recipient_id=? ORDER BY created_at DESC, notification_id LIMIT ? OFFSET ?",
                (r, i) -> new Notification(r.getObject("notification_id",UUID.class), recipient,
                        r.getObject("question_id",UUID.class),r.getObject("version_id",UUID.class),r.getString("type"),
                        r.getString("message"),r.getTimestamp("created_at").toInstant(),
                        r.getTimestamp("read_at")==null?null:r.getTimestamp("read_at").toInstant()), recipient,size,(long)page*size);
        long count = jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=?",Long.class,recipient);
        return PageResult.of(rows,page,size,count);
    }

    public long unreadCount(UUID recipient) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=? AND read_at IS NULL",Long.class,recipient);
    }

    public boolean markRead(UUID id, UUID recipient) {
        return jdbc.update("UPDATE notifications SET read_at=COALESCE(read_at,now()) WHERE notification_id=? AND recipient_id=?",id,recipient)>0;
    }
}
