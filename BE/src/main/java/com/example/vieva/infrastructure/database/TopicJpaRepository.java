package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, UUID> {
    @EntityGraph(attributePaths = {"subject"})
    List<TopicJpaEntity> findBySubject_SubjectIdOrderByOrderIndexAsc(UUID subjectId);

    @EntityGraph(attributePaths = {"subject"})
    List<TopicJpaEntity> findBySubject_SubjectIdIn(Collection<UUID> subjectIds);
    boolean existsByTopicIdAndSubject_SubjectId(UUID topicId, UUID subjectId);
}
