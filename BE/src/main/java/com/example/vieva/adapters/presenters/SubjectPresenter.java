package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.domain.entities.Subject;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubjectPresenter {

    public SubjectDto toDto(Subject subject) {
        if (subject == null) {
            return null;
        }
        return SubjectDto.builder()
                .subjectId(subject.getSubjectId())
                .headLecturerId(subject.getHeadLecturerId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .description(subject.getDescription())
                .credits(subject.getCredits())
                .status(subject.getStatus())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }

    public List<SubjectDto> toDtoList(List<Subject> subjects) {
        if (subjects == null) {
            return Collections.emptyList();
        }
        return subjects.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PageResponse<SubjectDto> toPageResponse(PagedResult<Subject> pagedResult) {
        return PageResponse.from(pagedResult, this::toDto);
    }
}
