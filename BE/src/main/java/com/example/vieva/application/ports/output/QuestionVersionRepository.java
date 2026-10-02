package com.example.vieva.application.ports.output;

import com.example.vieva.application.ports.input.QuestionVersionSearchCriteria;
import com.example.vieva.domain.entities.QuestionVersion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionVersionRepository {
    QuestionVersion save(QuestionVersion version);
    List<QuestionVersion> saveAll(List<QuestionVersion> versions);
    Optional<QuestionVersion> findById(UUID versionId);
    List<QuestionVersion> findAllByIds(Collection<UUID> versionIds);
    /** All versions of a question, newest first. */
    List<QuestionVersion> findByQuestionId(UUID questionId);
    List<QuestionVersion> findByQuestionIds(Collection<UUID> questionIds);
    Optional<QuestionVersion> findLatestVersion(UUID questionId);
    boolean hasDraftVersion(UUID questionId);
    List<QuestionVersion> findByGenerationRequestId(UUID generationRequestId);
    PagedResult<QuestionVersion> search(QuestionVersionSearchCriteria criteria);
    /**
     * Question texts already in the subject (every non-rejected version), used for the basic
     * near-duplicate check of generated drafts.
     */
    List<String> findActiveContentsBySubject(UUID subjectId);
    void deleteById(UUID versionId);
}
