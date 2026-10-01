package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.input.SubjectSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.domain.entities.Subject;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectRepositoryImpl implements SubjectRepository {

    private final SubjectJpaRepository subjectJpaRepository;
    private final SubjectPersistenceMapper subjectMapper;

    @Override
    @Transactional
    public Subject save(Subject subject) {
        SubjectJpaEntity entity = subjectMapper.toEntity(subject);
        SubjectJpaEntity saved = subjectJpaRepository.save(entity);
        return subjectMapper.toDomain(saved);
    }

    @Override
    public Optional<Subject> findById(UUID subjectId) {
        return subjectJpaRepository.findById(subjectId)
                .map(subjectMapper::toDomain);
    }

    @Override
    public Optional<Subject> findBySubjectCode(String subjectCode) {
        return subjectJpaRepository.findBySubjectCode(subjectCode)
                .map(subjectMapper::toDomain);
    }

    @Override
    public boolean existsBySubjectCode(String subjectCode) {
        return subjectJpaRepository.existsBySubjectCode(subjectCode);
    }

    @Override
    public PagedResult<Subject> findAll(SubjectSearchCriteria criteria) {
        int page = criteria.getSanitizedPage();
        int size = criteria.getSanitizedSize();
        String sortBy = criteria.getSanitizedSortBy();
        Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Specification<SubjectJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (StringUtils.hasText(criteria.getKeyword())) {
                String pattern = "%" + criteria.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("subjectCode")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("subjectName")), pattern);
                predicates.add(cb.or(codeMatch, nameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SubjectJpaEntity> entityPage = subjectJpaRepository.findAll(spec, pageRequest);
        List<Subject> content = entityPage.getContent().stream()
                .map(subjectMapper::toDomain)
                .collect(Collectors.toList());

        return PagedResult.<Subject>builder()
                .content(content)
                .page(entityPage.getNumber())
                .size(entityPage.getSize())
                .totalElements(entityPage.getTotalElements())
                .totalPages(entityPage.getTotalPages())
                .isFirst(entityPage.isFirst())
                .isLast(entityPage.isLast())
                .build();
    }
}
