package com.example.vieva.application.ports.output;

/** Rubric information an AI grader can quote in its prompt (name and description only). */
public record RubricSummary(String name, String description) {
}
