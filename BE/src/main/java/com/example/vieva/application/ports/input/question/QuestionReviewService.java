package com.example.vieva.application.ports.input.question;

import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.common.QuestionVersionSearchCriteria;

import java.util.List;
import java.util.UUID;

/**
 * UC1.3 review: the review queue, approval (the only path into the official bank — BR-05) and rejection.
 */
public interface QuestionReviewService {
    QuestionVersionView submitForReview(UUID versionId, Long expectedVersion, UUID actorId);
    QuestionVersionView submitForReview(UUID questionId, UUID versionId, Long expectedVersion, UUID actorId);

    QuestionVersionView reviewQuestion(UUID versionId, UUID actorId);
    QuestionVersionView reviewQuestion(UUID questionId, UUID versionId, UUID actorId);

    PageResult<QuestionVersionView> searchVersions(QuestionVersionSearchCriteria criteria);

    QuestionVersionView getVersion(UUID versionId);

    QuestionVersionView approve(UUID versionId, Long expectedVersion, UUID actorId);
    QuestionVersionView approve(UUID questionId, UUID versionId, Long expectedVersion, UUID actorId);

    List<QuestionVersionView> batchApprove(List<UUID> versionIds, UUID actorId);

    QuestionVersionView reject(UUID versionId, String reason, Long expectedVersion, UUID actorId);
    QuestionVersionView reject(UUID questionId, UUID versionId, String reason, Long expectedVersion, UUID actorId);
}
