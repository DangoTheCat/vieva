package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionVersion;

import java.util.List;
import java.util.Map;

/** Query mapping boundary for question version views. */
public interface QuestionVersionViewAssemblerPort {
    QuestionVersionView assemble(QuestionVersion version);
    List<QuestionVersionView> assemble(List<QuestionVersion> versions);
    List<QuestionVersionView> assemble(List<QuestionVersion> versions, Map<java.util.UUID, Question> questions);
}
