package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Persists brand-new DRAFT questions (manual, import, AI) in batches: one round-trip per table
 * instead of one per question. Must run inside the caller's transaction.
 */
@Component
@RequiredArgsConstructor
public class QuestionDraftWriter {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuestionSourceRepository questionSourceRepository;

    public record NewDraft(Question question, QuestionVersion version, Rubric rubric,
                           List<RubricCriterion> criteria, List<QuestionSource> sources) {
    }

    public void persist(List<NewDraft> drafts) {
        if (drafts.isEmpty()) {
            return;
        }
        List<Rubric> rubrics = new ArrayList<>();
        List<RubricCriterion> criteria = new ArrayList<>();
        List<QuestionSource> sources = new ArrayList<>();
        for (NewDraft draft : drafts) {
            if (draft.rubric() != null) {
                rubrics.add(draft.rubric());
            }
            criteria.addAll(draft.criteria() == null ? List.of() : draft.criteria());
            sources.addAll(draft.sources() == null ? List.of() : draft.sources());
        }
        questionRepository.saveAll(drafts.stream().map(NewDraft::question).filter(Objects::nonNull).toList());
        questionVersionRepository.saveAll(drafts.stream().map(NewDraft::version).toList());
        rubricRepository.saveAll(rubrics);
        rubricCriterionRepository.saveAll(criteria);
        questionSourceRepository.saveAll(sources);
    }
}
