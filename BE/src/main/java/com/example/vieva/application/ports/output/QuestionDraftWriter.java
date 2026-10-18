package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.DocumentStatus;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;

import java.util.List;

/** Persistence boundary for draft and document workflow writes. */
public interface QuestionDraftWriter {
    CourseDocument saveCourseDocument(CourseDocument document);
    CourseDocument updateDocumentStatus(CourseDocument document, DocumentStatus status, int chunks, String summary);
    QuestionVersion updateStatus(QuestionVersion version);
    QuestionVersionView fetchQuestionWithRubric(QuestionVersion version);
    void saveQuestionDraft(List<NewDraft> drafts);

    record NewDraft(Question question, QuestionVersion version, Rubric rubric,
                    List<RubricCriterion> criteria, List<QuestionSource> sources) { }
}
