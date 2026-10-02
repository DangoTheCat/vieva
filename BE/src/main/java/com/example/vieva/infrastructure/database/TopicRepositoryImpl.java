package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.domain.entities.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TopicRepositoryImpl implements TopicRepository {

    private final TopicJpaRepository jpaRepository;
    private final TopicPersistenceMapper mapper;

    @Override
    public Topic save(Topic topic) {
        TopicJpaEntity entity = mapper.toEntity(topic);
        TopicJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Topic> findById(UUID topicId) {
        return jpaRepository.findById(topicId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Topic> findBySubjectId(UUID subjectId) {
        return jpaRepository.findBySubject_SubjectIdOrderByOrderIndexAsc(subjectId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByIdAndSubjectId(UUID topicId, UUID subjectId) {
        return jpaRepository.existsByTopicIdAndSubject_SubjectId(topicId, subjectId);
    }
}
