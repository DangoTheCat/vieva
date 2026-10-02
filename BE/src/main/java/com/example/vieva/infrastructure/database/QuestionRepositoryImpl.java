package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.input.QuestionSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QuestionRepositoryImpl implements QuestionRepository {

    private final QuestionJpaRepository jpaRepository;
    private final QuestionPersistenceMapper mapper;

    @Override
    public Question save(Question question) {
        QuestionJpaEntity entity = mapper.toEntity(question);
        QuestionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Question> saveAll(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        List<QuestionJpaEntity> entities = questions.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Question> findById(UUID questionId) {
        return jpaRepository.findById(questionId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Question> findByCode(String questionCode) {
        return jpaRepository.findByQuestionCode(questionCode)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String questionCode) {
        return jpaRepository.existsByQuestionCode(questionCode);
    }

    @Override
    public PagedResult<Question> search(QuestionSearchCriteria criteria) {
        int pageNumber = Math.max(0, criteria.getPage());
        int pageSize = criteria.getSize() > 0 ? criteria.getSize() : 20;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

        Specification<QuestionJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getSubjectId() != null) {
                predicates.add(cb.equal(root.get("topic").get("subject").get("subjectId"), criteria.getSubjectId()));
            }

            if (criteria.getTopicId() != null) {
                predicates.add(cb.equal(root.get("topic").get("topicId"), criteria.getTopicId()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (StringUtils.hasText(criteria.getKeyword())) {
                String kw = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("questionCode")), kw);

                Subquery<UUID> contentSubquery = query.subquery(UUID.class);
                Root<QuestionVersionJpaEntity> vRoot = contentSubquery.from(QuestionVersionJpaEntity.class);
                contentSubquery.select(vRoot.get("question").get("questionId"));
                contentSubquery.where(cb.like(cb.lower(vRoot.get("questionContent")), kw));

                predicates.add(cb.or(codeMatch, root.get("questionId").in(contentSubquery)));
            }

            if (criteria.getApprovalStatus() != null || criteria.getBloomLevel() != null) {
                Subquery<UUID> versionSubquery = query.subquery(UUID.class);
                Root<QuestionVersionJpaEntity> vRoot = versionSubquery.from(QuestionVersionJpaEntity.class);
                versionSubquery.select(vRoot.get("question").get("questionId"));
                List<Predicate> vPreds = new ArrayList<>();

                if (criteria.getApprovalStatus() != null) {
                    vPreds.add(cb.equal(vRoot.get("approvalStatus"), criteria.getApprovalStatus()));
                }
                if (criteria.getBloomLevel() != null) {
                    vPreds.add(cb.equal(vRoot.get("bloomLevel"), criteria.getBloomLevel()));
                }
                versionSubquery.where(vPreds.toArray(new Predicate[0]));
                predicates.add(root.get("questionId").in(versionSubquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<QuestionJpaEntity> entityPage = jpaRepository.findAll(spec, pageRequest);
        List<Question> content = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());

        return PagedResult.<Question>builder()
                .content(content)
                .page(entityPage.getNumber())
                .size(entityPage.getSize())
                .totalElements(entityPage.getTotalElements())
                .totalPages(entityPage.getTotalPages())
                .build();
    }

    @Override
    public void updateStatus(UUID questionId, QuestionStatus status) {
        jpaRepository.findById(questionId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setUpdatedAt(Instant.now());
            jpaRepository.save(entity);
        });
    }

    @Override
    public void deleteById(UUID questionId) {
        jpaRepository.deleteById(questionId);
    }
}
