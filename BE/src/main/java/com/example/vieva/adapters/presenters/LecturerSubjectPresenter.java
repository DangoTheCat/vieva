package com.example.vieva.adapters.presenters;

import com.example.vieva.domain.entities.LecturerSubject;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LecturerSubjectPresenter {

    public LecturerSubjectDto toDto(LecturerSubject entity) {
        if (entity == null) {
            return null;
        }
        return LecturerSubjectDto.builder()
                .lecturerSubjectId(entity.getLecturerSubjectId())
                .lecturerId(entity.getLecturerId())
                .subjectId(entity.getSubjectId())
                .isActive(entity.getIsActive())
                .assignedAt(entity.getAssignedAt())
                .assignedBy(entity.getAssignedBy())
                .revokedAt(entity.getRevokedAt())
                .build();
    }

    public List<LecturerSubjectDto> toDtoList(List<LecturerSubject> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }
}
