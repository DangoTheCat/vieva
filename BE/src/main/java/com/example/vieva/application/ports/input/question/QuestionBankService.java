package com.example.vieva.application.ports.input.question;

import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.common.QuestionBankSearchCriteria;

import java.util.List;
import java.util.UUID;

/**
 * UC1.4: search the official bank (approved questions only), details, version history, archive.
 */
public interface QuestionBankService {
    PageResult<QuestionBankItemView> search(QuestionBankSearchCriteria criteria);

    QuestionDetailView getDetail(UUID questionId);

    List<QuestionVersionView> getHistory(UUID questionId);

    void archive(UUID questionId, UUID actorId);
}
