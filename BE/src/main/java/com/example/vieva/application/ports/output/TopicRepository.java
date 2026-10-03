package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Topic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository {
    Topic save(Topic topic);
    List<Topic> saveAll(List<Topic> topics);
    /**
     * Flushing variants that force SQL execution inside the call, so unique-index
     * violations surface here instead of at transaction commit (race handling).
     */
    Topic saveAndFlush(Topic topic);
    List<Topic> saveAllAndFlush(List<Topic> topics);
    Optional<Topic> findById(UUID topicId);
    List<Topic> findAllByIds(Collection<UUID> topicIds);
    List<Topic> findBySubjectId(UUID subjectId);
    List<Topic> findBySubjectIds(Collection<UUID> subjectIds);
    boolean existsByIdAndSubjectId(UUID topicId, UUID subjectId);
}
