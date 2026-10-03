package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionBankItemView;
import com.example.vieva.application.ports.output.QuestionDetailView;
import com.example.vieva.application.ports.output.QuestionVersionView;

import java.util.List;
import java.util.UUID;

/**
 * UC1.4: search the official bank (approved questions only), details, version history, archive.
 */
public interface QuestionBankService {
    PagedResult<QuestionBankItemView> search(QuestionBankSearchCriteria criteria);

    QuestionDetailView getDetail(UUID questionId);

    List<QuestionVersionView> getHistory(UUID questionId);

    void archive(UUID questionId, UUID actorId);
}
