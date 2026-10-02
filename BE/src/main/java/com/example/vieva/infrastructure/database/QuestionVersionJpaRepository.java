package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.QuestionApprovalStatus;
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
public interface QuestionVersionJpaRepository extends JpaRepository<QuestionVersionJpaEntity, UUID> {

    List<QuestionVersionJpaEntity> findByQuestion_QuestionIdOrderByVersionNumberDesc(UUID questionId);

    List<QuestionVersionJpaEntity> findByQuestion_QuestionIdIn(Collection<UUID> questionIds);

    Optional<QuestionVersionJpaEntity> findFirstByQuestion_QuestionIdAndApprovalStatusOrderByVersionNumberDesc(
            UUID questionId, QuestionApprovalStatus approvalStatus);

    Optional<QuestionVersionJpaEntity> findFirstByQuestion_QuestionIdOrderByVersionNumberDesc(UUID questionId);

    boolean existsByQuestion_QuestionIdAndApprovalStatus(UUID questionId, QuestionApprovalStatus approvalStatus);

    @Modifying
    @Query("UPDATE QuestionVersionJpaEntity v SET v.approvalStatus = com.example.vieva.domain.entities.QuestionApprovalStatus.SUPERSEDED WHERE v.question.questionId = :questionId AND v.approvalStatus = com.example.vieva.domain.entities.QuestionApprovalStatus.APPROVED AND v.questionVersionId != :currentVersionId")
    void supersedeOlderApprovedVersions(
            @Param("questionId") UUID questionId,
            @Param("currentVersionId") UUID currentVersionId);
}
