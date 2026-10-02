package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.input.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.domain.entities.Question;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QuestionRepositoryImpl implements QuestionRepository {

    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "questionCode");
    private static final int MAX_PAGE_SIZE = 100;

    private final QuestionJpaRepository jpaRepository;
    private final QuestionPersistenceMapper mapper;

    @Override
    public Question save(Question question) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(question)));
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
        return jpaRepository.findById(questionId).map(mapper::toDomain);
    }

    @Override
    public List<Question> findAllByIds(Collection<UUID> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(questionIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResult<Question> searchBank(QuestionBankSearchCriteria criteria) {
        int page = Math.max(0, criteria.page());
        int size = criteria.size() > 0 ? Math.min(criteria.size(), MAX_PAGE_SIZE) : 20;
        String sortField = criteria.sortBy() != null && SORTABLE.contains(criteria.sortBy()) ? criteria.sortBy() : "updatedAt";
        Sort sort = criteria.sortAscending() ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort.and(Sort.by("questionId")));

        Specification<QuestionJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Only questions published through UC1.3 belong to the bank.
            predicates.add(cb.isNotNull(root.get("currentApprovedVersionId")));
            predicates.add(cb.equal(root.get("subject").get("subjectId"), criteria.subjectId()));

            if (criteria.topicId() != null) {
                predicates.add(cb.equal(root.get("topic").get("topicId"), criteria.topicId()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            // Bloom and keyword are matched against the approved version in force.
            if (criteria.bloomLevel() != null) {
                Subquery<UUID> bloomMatches = query.subquery(UUID.class);
                Root<QuestionVersionJpaEntity> version = bloomMatches.from(QuestionVersionJpaEntity.class);
                bloomMatches.select(version.get("questionVersionId"))
                        .where(cb.equal(version.get("bloomLevel"), criteria.bloomLevel()));
                predicates.add(root.get("currentApprovedVersionId").in(bloomMatches));
            }
            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + escapeLike(criteria.keyword().trim().toLowerCase()) + "%";
                Subquery<UUID> textMatches = query.subquery(UUID.class);
                Root<QuestionVersionJpaEntity> version = textMatches.from(QuestionVersionJpaEntity.class);
                textMatches.select(version.get("questionVersionId"))
                        .where(cb.or(
                                cb.like(cb.lower(version.get("questionContent")), pattern, '\\'),
                                cb.like(cb.lower(version.get("referenceAnswer")), pattern, '\\')));
                predicates.add(cb.or(
                        root.get("currentApprovedVersionId").in(textMatches),
                        cb.like(cb.lower(root.get("questionCode")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<QuestionJpaEntity> result = jpaRepository.findAll(spec, pageRequest);
        List<Question> content = result.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return PagedResult.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public void deleteById(UUID questionId) {
        jpaRepository.deleteById(questionId);
    }

    static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
