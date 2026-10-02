package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDetailView {
    private Question question;
    private QuestionVersion activeVersion;
    private QuestionVersion draftVersion;
    private boolean hasPendingDraft;
    private Rubric rubric;
    private List<RubricCriterion> criteria;
    private List<QuestionSource> sources;
    private List<QuestionVersion> versionHistory;
}
