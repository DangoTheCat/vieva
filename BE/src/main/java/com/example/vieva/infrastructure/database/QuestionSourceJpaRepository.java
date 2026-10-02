package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionSourceJpaRepository extends JpaRepository<QuestionSourceJpaEntity, UUID> {

    List<QuestionSourceJpaEntity> findByQuestionVersion_QuestionVersionId(UUID questionVersionId);

    List<QuestionSourceJpaEntity> findByQuestionVersion_QuestionVersionIdIn(Collection<UUID> questionVersionIds);

    @Modifying
    @Query("DELETE FROM QuestionSourceJpaEntity s WHERE s.questionVersion.questionVersionId = :versionId")
    void deleteByVersionId(@Param("versionId") UUID versionId);
}
