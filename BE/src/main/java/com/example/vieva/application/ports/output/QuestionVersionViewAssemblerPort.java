package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionVersion;

import java.util.List;
import java.util.Map;

/** Query mapping boundary for question version aggregates. */
public interface QuestionVersionViewAssemblerPort {
    QuestionVersionAggregate assemble(QuestionVersion version);
    List<QuestionVersionAggregate> assemble(List<QuestionVersion> versions);
    List<QuestionVersionAggregate> assemble(List<QuestionVersion> versions, Map<java.util.UUID, Question> questions);
}
