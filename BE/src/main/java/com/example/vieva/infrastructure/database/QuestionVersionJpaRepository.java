package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.QuestionApprovalStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionVersionJpaRepository extends JpaRepository<QuestionVersionJpaEntity, UUID>,
        JpaSpecificationExecutor<QuestionVersionJpaEntity> {

    @EntityGraph(attributePaths = {"question"})
    List<QuestionVersionJpaEntity> findByQuestion_QuestionIdOrderByVersionNumberDesc(UUID questionId);

    @EntityGraph(attributePaths = {"question"})
    List<QuestionVersionJpaEntity> findByQuestion_QuestionIdIn(Collection<UUID> questionIds);

    @EntityGraph(attributePaths = {"question"})
    Optional<QuestionVersionJpaEntity> findFirstByQuestion_QuestionIdOrderByVersionNumberDesc(UUID questionId);

    @EntityGraph(attributePaths = {"question"})
    List<QuestionVersionJpaEntity> findByGenerationRequestIdOrderByCreatedAtAsc(UUID generationRequestId);

    boolean existsByQuestion_QuestionIdAndApprovalStatus(UUID questionId, QuestionApprovalStatus approvalStatus);

    @Query("""
            SELECT v.questionContent FROM QuestionVersionJpaEntity v
            WHERE v.question.subject.subjectId = :subjectId
              AND v.approvalStatus <> com.example.vieva.domain.entities.QuestionApprovalStatus.REJECTED
            """)
    List<String> findActiveContentsBySubject(@Param("subjectId") UUID subjectId);
}
