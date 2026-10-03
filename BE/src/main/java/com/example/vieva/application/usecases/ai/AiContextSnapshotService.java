package com.example.vieva.application.usecases.ai;

import com.example.vieva.application.ports.output.AiRuleRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.domain.entities.AiRule;
import com.example.vieva.domain.entities.AiRuleType;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.entities.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builds the academic context block injected at the top of AI system prompts: active subjects with
 * their topics, plus the active CONTEXT_FILTER rules (exam regulations, rubric matrix) and the
 * grounding rule. Refreshed every 60s by {@code AiContextSnapshotScheduler}; a failed refresh keeps
 * the previous snapshot.
 */
@Service
@RequiredArgsConstructor
public class AiContextSnapshotService {

    static final String HEADER = "=== DỮ LIỆU THỰC TẾ HỆ THỐNG AIVES (SNAPSHOT 60S) ===";
    static final String FOOTER = "=== HẾT SNAPSHOT ===";
    static final String GROUNDING_RULE = "QUY TẮC GROUNDING: Chỉ trả lời dựa trên dữ liệu trong khối Snapshot này. "
            + "Thông tin không có trong Snapshot thì trả lời rằng hệ thống chưa có dữ liệu, không suy đoán.";

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final AiRuleRepository aiRuleRepository;

    private final AtomicReference<CachedSnapshot> cache = new AtomicReference<>();

    public record CachedSnapshot(String content, Instant generatedAt) {
    }

    /** Latest snapshot; built on demand when the scheduler has not run yet. */
    public CachedSnapshot current() {
        CachedSnapshot snapshot = cache.get();
        return snapshot != null ? snapshot : refresh();
    }

    /** Rebuilds the snapshot from the database. The cache is replaced only on success. */
    public CachedSnapshot refresh() {
        Instant now = Instant.now();
        CachedSnapshot fresh = new CachedSnapshot(build(now), now);
        cache.set(fresh);
        return fresh;
    }

    private String build(Instant generatedAt) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        sb.append("Thời điểm cập nhật: ").append(generatedAt).append("\n\n");

        sb.append("[MÔN HỌC ĐANG MỞ]\n");
        List<Subject> subjects = subjectRepository.findAllByStatus(SubjectStatus.ACTIVE);
        if (subjects.isEmpty()) {
            sb.append("- Chưa có môn học nào đang mở.\n");
        }
        for (Subject subject : subjects) {
            appendSubject(sb, subject, topicRepository.findBySubjectId(subject.getSubjectId()));
        }

        for (AiRule rule : aiRuleRepository.findActiveByType(AiRuleType.CONTEXT_FILTER)) {
            sb.append('\n').append(rule.getPromptContent().strip()).append('\n');
        }

        sb.append('\n').append(GROUNDING_RULE).append('\n');
        sb.append(FOOTER);
        return sb.toString();
    }

    private static void appendSubject(StringBuilder sb, Subject subject, List<Topic> topics) {
        sb.append("- ").append(subject.getSubjectCode()).append(" — ").append(subject.getSubjectName());
        if (subject.getCredits() != null) {
            sb.append(" (").append(subject.getCredits()).append(" tín chỉ)");
        }
        sb.append('\n');
        if (subject.getDescription() != null && !subject.getDescription().isBlank()) {
            sb.append("  Mô tả: ").append(subject.getDescription().strip()).append('\n');
        }
        if (topics.isEmpty()) {
            return;
        }
        sb.append("  Chủ đề kiến thức:\n");
        int index = 1;
        for (Topic topic : topics) {
            sb.append("    ").append(index++).append(". ").append(topic.getTopicName());
            if (topic.getDescription() != null && !topic.getDescription().isBlank()) {
                sb.append(": ").append(topic.getDescription().strip());
            }
            sb.append('\n');
        }
    }
}
