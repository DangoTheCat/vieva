package com.example.vieva.infrastructure.database;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, UUID>, JpaSpecificationExecutor<QuestionJpaEntity> {

    /**
     * Specification search with the owning subject/topic fetched in the same round-trip,
     * so mapping a page of rows does not trigger per-row lazy loads (Rule 4: N+1).
     */
    @Override
    @EntityGraph(attributePaths = {"subject", "topic"})
    Page<QuestionJpaEntity> findAll(@Nullable Specification<QuestionJpaEntity> spec, Pageable pageable);
}
