package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RubricJpaRepository extends JpaRepository<RubricJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"questionVersion"})
    Optional<RubricJpaEntity> findByQuestionVersion_QuestionVersionId(UUID questionVersionId);

    @EntityGraph(attributePaths = {"questionVersion"})
    List<RubricJpaEntity> findByQuestionVersion_QuestionVersionIdIn(Collection<UUID> questionVersionIds);

    @Modifying
    @Query("DELETE FROM RubricJpaEntity r WHERE r.questionVersion.questionVersionId = :versionId")
    void deleteByVersionId(@Param("versionId") UUID versionId);
}
