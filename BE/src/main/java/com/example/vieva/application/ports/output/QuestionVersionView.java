package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.entities.Topic;
import lombok.Builder;

import java.util.List;

/**
 * One version with everything a reviewer needs: owning question, topic, rubric, criteria, sources.
 */
@Builder
public record QuestionVersionView(
        Question question,
        Topic topic,
        QuestionVersion version,
        Rubric rubric,
        List<RubricCriterion> criteria,
        List<QuestionSource> sources
) {
}
