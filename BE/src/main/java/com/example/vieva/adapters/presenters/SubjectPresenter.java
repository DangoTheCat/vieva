package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.input.subject.SubjectResult;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubjectPresenter {

    public SubjectDto toDto(SubjectResult subject) {
        if (subject == null) {
            return null;
        }
        return SubjectDto.builder()
                .subjectId(subject.getSubjectId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .description(subject.getDescription())
                .credits(subject.getCredits())
                .status(subject.getStatus())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }

    public List<SubjectDto> toDtoList(List<SubjectResult> subjects) {
        if (subjects == null) {
            return Collections.emptyList();
        }
        return subjects.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
