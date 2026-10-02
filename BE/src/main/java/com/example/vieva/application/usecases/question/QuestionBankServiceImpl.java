package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.ports.output.QuestionBankItemView;
import com.example.vieva.application.ports.output.QuestionDetailView;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionApprovalStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionAccessLoader accessLoader;
    private final QuestionVersionViewAssembler viewAssembler;
    private final QuestionBankAuditor auditor;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<QuestionBankItemView> search(QuestionBankSearchCriteria criteria) {
        PagedResult<Question> page = questionRepository.searchBank(criteria);
        List<Question> questions = page.getContent();
        Map<UUID, Question> questionsById = questions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));

        // One query for every version of the page: current approved versions + pending-draft flags.
        List<QuestionVersion> versions = questionVersionRepository.findByQuestionIds(questionsById.keySet());
        Set<UUID> currentIds = questions.stream()
                .map(Question::getCurrentApprovedVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<UUID> withDraft = versions.stream()
                .filter(v -> v.getApprovalStatus() == QuestionApprovalStatus.DRAFT)
                .map(QuestionVersion::getQuestionId)
                .collect(Collectors.toSet());
        Map<UUID, QuestionVersionView> viewsByQuestion = viewAssembler.assemble(
                        versions.stream().filter(v -> currentIds.contains(v.getQuestionVersionId())).toList(),
                        questionsById).stream()
                .collect(Collectors.toMap(view -> view.question().getQuestionId(), Function.identity()));

        List<QuestionBankItemView> items = questions.stream()
                .filter(q -> viewsByQuestion.containsKey(q.getQuestionId()))
                .map(q -> new QuestionBankItemView(viewsByQuestion.get(q.getQuestionId()), withDraft.contains(q.getQuestionId())))
                .toList();
        return PagedResult.of(items, page.getPage(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDetailView getDetail(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        List<QuestionVersion> history = questionVersionRepository.findByQuestionId(questionId);
        QuestionVersion current = history.stream()
                .filter(v -> v.getQuestionVersionId().equals(question.getCurrentApprovedVersionId()))
                .findFirst()
                .orElse(null);
        QuestionVersion draft = history.stream()
                .filter(QuestionVersion::isDraft)
                .findFirst()
                .orElse(null);
        QuestionVersionView currentView = current == null ? null
                : viewAssembler.assemble(List.of(current), Map.of(questionId, question)).get(0);
        return QuestionDetailView.builder()
                .question(question)
                .currentVersion(currentView)
                .pendingDraft(draft)
                .history(history)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionVersionView> getHistory(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        return viewAssembler.assemble(questionVersionRepository.findByQuestionId(questionId), Map.of(questionId, question));
    }

    @Override
    @Transactional
    public void archive(UUID questionId, UUID actorId) {
        Question question = accessLoader.questionForWrite(questionId, actorId);
        question.archive();
        questionRepository.save(question);
        auditor.record(actorId, "QUESTION_ARCHIVED", QuestionBankAuditor.QUESTION, questionId,
                Map.of("status", "ACTIVE"), Map.of("status", "ARCHIVED"));
    }
}
