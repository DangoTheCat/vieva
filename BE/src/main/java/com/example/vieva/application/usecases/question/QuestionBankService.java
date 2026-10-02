package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CreateManualQuestionRequest;
import com.example.vieva.application.ports.input.GenerateQuestionsRagRequest;
import com.example.vieva.application.ports.input.QuestionSearchCriteria;
import com.example.vieva.application.ports.input.UpdateQuestionDraftRequest;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionDetailView;

import java.util.List;
import java.util.UUID;

public interface QuestionBankService {
    List<QuestionDetailView> generateQuestionsViaRag(UUID subjectId, GenerateQuestionsRagRequest request, UUID lecturerId);
    QuestionDetailView createManualQuestion(UUID subjectId, CreateManualQuestionRequest request, UUID lecturerId);
    QuestionDetailView createDraftFromApproved(UUID questionId, UUID lecturerId);
    QuestionDetailView updateDraftVersion(UUID questionId, UUID versionId, UpdateQuestionDraftRequest request, UUID lecturerId);
    QuestionDetailView approveQuestionVersion(UUID questionId, UUID versionId, UUID lecturerId);
    QuestionDetailView rejectQuestionVersion(UUID questionId, UUID versionId, String reason, UUID lecturerId);
    void deleteDraftVersion(UUID questionId, UUID versionId, UUID lecturerId);
    void archiveQuestion(UUID questionId, UUID lecturerId);
    void restoreQuestion(UUID questionId, UUID lecturerId);
    PagedResult<QuestionDetailView> searchQuestions(QuestionSearchCriteria criteria);
    QuestionDetailView getQuestionDetails(UUID questionId);
}
