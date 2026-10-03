package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.input.QuestionVersionSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QuestionVersionRepositoryImpl implements QuestionVersionRepository {

    private static final int MAX_PAGE_SIZE = 100;

    private final QuestionVersionJpaRepository jpaRepository;
    private final QuestionJpaRepository questionJpaRepository;
    private final QuestionVersionPersistenceMapper mapper;

    @Override
    @Transactional
    public QuestionVersion save(QuestionVersion version) {
        // saveAndFlush surfaces optimistic-lock and unique-index conflicts inside the use case.
        return mapper.toDomain(jpaRepository.saveAndFlush(toEntity(version)));
    }

    @Override
    @Transactional
    public List<QuestionVersion> saveAll(List<QuestionVersion> versions) {
        if (versions == null || versions.isEmpty()) {
            return List.of();
        }
        List<QuestionVersionJpaEntity> entities = versions.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<QuestionVersion> findById(UUID versionId) {
        return jpaRepository.findById(versionId).map(mapper::toDomain);
    }

    @Override
    public List<QuestionVersion> findAllByIds(Collection<UUID> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(versionIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionVersion> findByQuestionId(UUID questionId) {
        return jpaRepository.findByQuestion_QuestionIdOrderByVersionNumberDesc(questionId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionVersion> findByQuestionIds(Collection<UUID> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByQuestion_QuestionIdIn(questionIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<QuestionVersion> findLatestVersion(UUID questionId) {
        return jpaRepository.findFirstByQuestion_QuestionIdOrderByVersionNumberDesc(questionId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean hasDraftVersion(UUID questionId) {
        return jpaRepository.existsByQuestion_QuestionIdAndApprovalStatus(questionId, QuestionApprovalStatus.DRAFT);
    }

    @Override
    public List<QuestionVersion> findByGenerationRequestId(UUID generationRequestId) {
        return jpaRepository.findByGenerationRequestIdOrderByCreatedAtAsc(generationRequestId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResult<QuestionVersion> search(QuestionVersionSearchCriteria criteria) {
        int page = Math.max(0, criteria.page());
        int size = criteria.size() > 0 ? Math.min(criteria.size(), MAX_PAGE_SIZE) : 20;
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by("updatedAt").descending().and(Sort.by("questionVersionId")));

        Specification<QuestionVersionJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("question").get("subject").get("subjectId"), criteria.subjectId()));
            if (criteria.topicId() != null) {
                predicates.add(cb.equal(root.get("question").get("topic").get("topicId"), criteria.topicId()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), criteria.status()));
            }
            if (criteria.origin() != null) {
                predicates.add(cb.equal(root.get("generationMode"), criteria.origin()));
            }
            if (criteria.bloomLevel() != null) {
                predicates.add(cb.equal(root.get("bloomLevel"), criteria.bloomLevel()));
            }
            if (criteria.generationRequestId() != null) {
                predicates.add(cb.equal(root.get("generationRequestId"), criteria.generationRequestId()));
            }
            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + QuestionRepositoryImpl.escapeLike(criteria.keyword().trim().toLowerCase()) + "%";
                predicates.add(cb.like(cb.lower(root.get("questionContent")), pattern, '\\'));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<QuestionVersionJpaEntity> result = jpaRepository.findAll(spec, pageRequest);
        List<QuestionVersion> content = result.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return PagedResult.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public List<String> findActiveContentsBySubject(UUID subjectId) {
        return jpaRepository.findActiveContentsBySubject(subjectId);
    }

    @Override
    public void deleteById(UUID versionId) {
        jpaRepository.deleteById(versionId);
    }

    /**
     * QuestionJpaEntity is versioned: an id-only stub (version = null) would look transient to
     * Hibernate, so the association is a managed reference instead.
     */
    private QuestionVersionJpaEntity toEntity(QuestionVersion version) {
        QuestionVersionJpaEntity entity = mapper.toEntity(version);
        if (version.getQuestionId() != null) {
            entity.setQuestion(questionJpaRepository.getReferenceById(version.getQuestionId()));
        }
        return entity;
    }
}
