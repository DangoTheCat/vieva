package com.example.vieva.application.usecases.subject;

import com.example.vieva.application.ports.input.subject.SubjectResult;
import com.example.vieva.domain.entities.Subject;

public final class SubjectResults {

    private SubjectResults() {
    }

    public static SubjectResult from(Subject subject) {
        if (subject == null) {
            return null;
        }
        return SubjectResult.builder()
                .subjectId(subject.getSubjectId())
                .headLecturerId(subject.getHeadLecturerId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .description(subject.getDescription())
                .credits(subject.getCredits())
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
