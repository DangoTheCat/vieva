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
        if (pagedResult == null) {
            return null;
        }
        return PageResponse.<SubjectDto>builder()
                .content(toDtoList(pagedResult.getContent()))
                .page(pagedResult.getPage())
                .size(pagedResult.getSize())
                .totalElements(pagedResult.getTotalElements())
                .totalPages(pagedResult.getTotalPages())
                .isFirst(pagedResult.isFirst())
                .isLast(pagedResult.isLast())
                .build();
    }
}
