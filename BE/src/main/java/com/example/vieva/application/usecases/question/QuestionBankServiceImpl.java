package com.example.vieva.application.usecases.question;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.input.question.QuestionBankItemView;
import com.example.vieva.application.ports.common.QuestionBankSearchCriteria;
import com.example.vieva.application.ports.input.question.QuestionBankService;
import com.example.vieva.application.ports.input.question.QuestionDetailView;
import com.example.vieva.application.ports.input.question.QuestionVersionView;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionAggregate;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.QuestionVersionViewAssemblerPort;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.common.EnumValues;
import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionLifecycleStatus;
import com.example.vieva.domain.entities.QuestionStatus;
import com.example.vieva.domain.entities.QuestionVersion;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionAccessLoader accessLoader;
    private final QuestionVersionViewAssemblerPort viewAssembler;
    private final QuestionBankAuditor auditor;

    @Override
    @UseCaseTransaction(readOnly = true)
    public PageResult<QuestionBankItemView> search(QuestionBankSearchCriteria criteria) {
        EnumValues.parse(BloomLevel.class, criteria.bloomLevel(), "Invalid value for parameter 'bloomLevel'");
        EnumValues.parse(QuestionLifecycleStatus.class, criteria.status(), "Invalid value for parameter 'status'");
        PageResult<Question> page = questionRepository.searchBank(criteria);
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
                .filter(v -> v.getApprovalStatus() == QuestionStatus.DRAFT || v.getApprovalStatus() == QuestionStatus.UNDER_REVIEW)
                .map(QuestionVersion::getQuestionId)
                .collect(Collectors.toSet());
        Map<UUID, QuestionVersionAggregate> aggregatesByQuestion = viewAssembler.assemble(
                        versions.stream().filter(v -> currentIds.contains(v.getQuestionVersionId())).toList(),
                        questionsById).stream()
                .collect(Collectors.toMap(agg -> agg.question().getQuestionId(), Function.identity()));

        List<QuestionBankItemView> items = questions.stream()
                .filter(q -> aggregatesByQuestion.containsKey(q.getQuestionId()))
                .map(q -> new QuestionBankItemView(
                        QuestionViewMapper.toView(aggregatesByQuestion.get(q.getQuestionId())),
                        withDraft.contains(q.getQuestionId())))
                .toList();
        return PageResult.of(items, page.getPage(), page.getSize(), page.getTotalElements());
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public QuestionDetailView getDetail(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_NOT_FOUND));
        List<QuestionVersion> history = questionVersionRepository.findByQuestionId(questionId);
        QuestionVersion current = history.stream()
                .filter(v -> v.getQuestionVersionId().equals(question.getCurrentApprovedVersionId()))
                .findFirst()
                .orElse(null);
        QuestionVersion draft = history.stream()
                .filter(QuestionVersion::isDraft)
                .findFirst()
                .orElse(null);
        QuestionVersionAggregate currentAggregate = current == null ? null
                : viewAssembler.assemble(List.of(current), Map.of(questionId, question)).get(0);
        return QuestionDetailView.builder()
                .question(QuestionViewMapper.toResult(question))
                .currentVersion(QuestionViewMapper.toView(currentAggregate))
                .pendingDraft(QuestionViewMapper.toResult(draft))
                .history(history == null ? List.of() : history.stream().map(QuestionViewMapper::toResult).toList())
                .build();
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<QuestionVersionView> getHistory(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_NOT_FOUND));
        List<QuestionVersionAggregate> aggregates = viewAssembler.assemble(
                questionVersionRepository.findByQuestionId(questionId), Map.of(questionId, question));
        return QuestionViewMapper.toViews(aggregates);
    }

    @Override
    @UseCaseTransaction
    public void archive(UUID questionId, UUID actorId) {
        Question question = accessLoader.questionForWrite(questionId, actorId);
        question.archive();
        questionRepository.save(question);
        auditor.record(actorId, "QUESTION_ARCHIVED", QuestionBankAuditor.QUESTION, questionId,
                Map.of("status", "ACTIVE"), Map.of("status", "ARCHIVED"));
    }
}
