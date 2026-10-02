package com.example.vieva.application.usecases.catalog;

import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.LecturerSubject;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.Topic;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LecturerCatalogServiceImpl implements LecturerCatalogService {

    private static final int MAX_TOPIC_NAME = 255;

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubjectAccessGuard subjectAccessGuard;
    private final QuestionBankAuditor auditor;

    @Override
    @Transactional(readOnly = true)
    public List<Subject> listAssignedSubjects(UUID lecturerId) {
        List<UUID> subjectIds = lecturerSubjectRepository.findActiveByLecturerId(lecturerId).stream()
                .map(LecturerSubject::getSubjectId)
                .distinct()
                .toList();
        return subjectRepository.findAllByIds(subjectIds).stream()
                .sorted(Comparator.comparing(Subject::getSubjectCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> listTopics(UUID subjectId) {
        if (subjectRepository.findById(subjectId).isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        return topicRepository.findBySubjectId(subjectId);
    }

    @Override
    @Transactional
    public Topic createTopic(UUID subjectId, String name, String description, UUID actorId) {
        subjectAccessGuard.requireManage(actorId, subjectId);
        if (subjectRepository.findById(subjectId).isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        if (name == null || name.isBlank() || name.trim().length() > MAX_TOPIC_NAME) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Topic name is required (max " + MAX_TOPIC_NAME + " characters)");
        }
        String trimmed = name.trim();
        List<Topic> existing = topicRepository.findBySubjectId(subjectId);
        if (existing.stream().anyMatch(t -> t.getTopicName() != null && t.getTopicName().equalsIgnoreCase(trimmed))) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Topic '" + trimmed + "' already exists in this subject");
        }
        int nextOrder = existing.stream()
                .map(Topic::getOrderIndex)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        Topic topic;
        try {
            topic = topicRepository.save(Topic.builder()
                    .topicId(UUID.randomUUID())
                    .subjectId(subjectId)
                    .topicName(trimmed)
                    .description(description == null ? null : description.trim())
                    .orderIndex(nextOrder)
                    .createdAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Concurrent creation with the same name won the race (unique index uq_topics_subject_lower_name)
            throw new AppException(ErrorCode.INVALID_REQUEST, "Topic '" + trimmed + "' already exists in this subject");
        }
        auditor.record(actorId, "TOPIC_CREATED", QuestionBankAuditor.SUBJECT, subjectId,
                Map.of("topicId", topic.getTopicId(), "topicName", trimmed));
        return topic;
    }
}
