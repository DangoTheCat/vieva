package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.QuestionVersion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionVersionRepository {
    QuestionVersion save(QuestionVersion version);
    List<QuestionVersion> saveAll(List<QuestionVersion> versions);
    Optional<QuestionVersion> findById(UUID versionId);
    List<QuestionVersion> findByQuestionId(UUID questionId);
    List<QuestionVersion> findByQuestionIds(Collection<UUID> questionIds);
    Optional<QuestionVersion> findActiveApprovedVersion(UUID questionId);
    Optional<QuestionVersion> findLatestDraftVersion(UUID questionId);
    Optional<QuestionVersion> findLatestVersion(UUID questionId);
    boolean hasDraftVersion(UUID questionId);
    void supersedeOlderApprovedVersions(UUID questionId, UUID currentApprovedVersionId);
    void deleteById(UUID versionId);
}
