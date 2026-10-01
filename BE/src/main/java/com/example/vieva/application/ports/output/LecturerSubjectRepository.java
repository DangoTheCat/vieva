package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.LecturerSubject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LecturerSubjectRepository {
    LecturerSubject save(LecturerSubject assignment);
    Optional<LecturerSubject> findById(UUID id);
    Optional<LecturerSubject> findActiveAssignment(UUID lecturerId, UUID subjectId);
    List<LecturerSubject> findActiveBySubjectId(UUID subjectId);
    List<LecturerSubject> findActiveByLecturerId(UUID lecturerId);
    boolean isLecturerAssignedToSubject(UUID lecturerId, UUID subjectId);
}
