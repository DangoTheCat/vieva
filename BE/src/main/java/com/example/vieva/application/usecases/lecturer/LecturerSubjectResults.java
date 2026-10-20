package com.example.vieva.application.usecases.lecturer;

import com.example.vieva.application.ports.input.lecturer.LecturerSubjectResult;
import com.example.vieva.domain.entities.LecturerSubject;

public final class LecturerSubjectResults {

    private LecturerSubjectResults() {
    }

    public static LecturerSubjectResult from(LecturerSubject entity) {
        if (entity == null) {
            return null;
        }
        return LecturerSubjectResult.builder()
                .lecturerSubjectId(entity.getLecturerSubjectId())
                .lecturerId(entity.getLecturerId())
                .subjectId(entity.getSubjectId())
                .isActive(entity.getIsActive())
                .assignedAt(entity.getAssignedAt())
                .assignedBy(entity.getAssignedBy())
                .revokedAt(entity.getRevokedAt())
                .build();
    }
}
