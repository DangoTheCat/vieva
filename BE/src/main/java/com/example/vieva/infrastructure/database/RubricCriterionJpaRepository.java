package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RubricCriterionJpaRepository extends JpaRepository<RubricCriterionJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"rubric"})
    List<RubricCriterionJpaEntity> findByRubric_RubricIdOrderByOrderIndexAsc(UUID rubricId);

    @EntityGraph(attributePaths = {"rubric"})
    List<RubricCriterionJpaEntity> findByRubric_RubricIdInOrderByOrderIndexAsc(Collection<UUID> rubricIds);

    @Modifying
    @Query("DELETE FROM RubricCriterionJpaEntity c WHERE c.rubric.rubricId = :rubricId")
    void deleteByRubricId(@Param("rubricId") UUID rubricId);
}
