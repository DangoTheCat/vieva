package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Topic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository {
    Topic save(Topic topic);
    List<Topic> saveAll(List<Topic> topics);
    Optional<Topic> findById(UUID topicId);
    List<Topic> findAllByIds(Collection<UUID> topicIds);
    List<Topic> findBySubjectId(UUID subjectId);
    boolean existsByIdAndSubjectId(UUID topicId, UUID subjectId);
}
