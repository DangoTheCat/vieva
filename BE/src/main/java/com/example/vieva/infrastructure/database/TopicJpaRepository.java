package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, UUID> {
    List<TopicJpaEntity> findBySubject_SubjectIdOrderByOrderIndexAsc(UUID subjectId);
    boolean existsByTopicIdAndSubject_SubjectId(UUID topicId, UUID subjectId);
}
