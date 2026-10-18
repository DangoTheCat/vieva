package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionDraftWriter;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.QuestionVersionView;
import com.example.vieva.application.ports.output.QuestionVersionViewAssemblerPort;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentStatus;
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
public class PostgreSQLDatabase implements QuestionDraftWriter {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuestionSourceRepository questionSourceRepository;
    private final CourseDocumentRepository documents;
    private final QuestionVersionViewAssemblerPort assembler;

    public CourseDocument saveCourseDocument(CourseDocument document) { return documents.save(document); }

    public CourseDocument updateDocumentStatus(CourseDocument document, DocumentStatus status, int chunks, String summary) {
        if (status == DocumentStatus.READY) document.markReady(chunks,summary);
        else if (status == DocumentStatus.ARCHIVED) document.archive();
        else throw new IllegalArgumentException("Unsupported document transition");
        return documents.save(document);
    }


    public QuestionVersion updateStatus(QuestionVersion version) { return questionVersionRepository.save(version); }

    public QuestionVersionView fetchQuestionWithRubric(QuestionVersion version) { return assembler.assemble(version); }

    public void saveQuestionDraft(List<QuestionDraftWriter.NewDraft> drafts) {
        if (drafts.isEmpty()) {
            return;
        }
        List<Rubric> rubrics = new ArrayList<>();
        List<RubricCriterion> criteria = new ArrayList<>();
        List<QuestionSource> sources = new ArrayList<>();
        for (QuestionDraftWriter.NewDraft draft : drafts) {
            if (draft.rubric() != null) {
                rubrics.add(draft.rubric());
            }
            criteria.addAll(draft.criteria() == null ? List.of() : draft.criteria());
            sources.addAll(draft.sources() == null ? List.of() : draft.sources());
        }
        questionRepository.saveAll(drafts.stream().map(QuestionDraftWriter.NewDraft::question).filter(Objects::nonNull).toList());
        questionVersionRepository.saveAll(drafts.stream().map(QuestionDraftWriter.NewDraft::version).toList());
        rubricRepository.saveAll(rubrics);
        rubricCriterionRepository.saveAll(criteria);
        questionSourceRepository.saveAll(sources);
    }
}
