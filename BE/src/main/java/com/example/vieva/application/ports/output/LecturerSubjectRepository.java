package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.LecturerSubject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LecturerSubjectRepository {
    LecturerSubject save(LecturerSubject assignment);
    List<LecturerSubject> saveAll(List<LecturerSubject> assignments);
    Optional<LecturerSubject> findById(UUID id);
    Optional<LecturerSubject> findActiveAssignment(UUID lecturerId, UUID subjectId);
    List<LecturerSubject> findActiveAssignments(UUID subjectId, Collection<UUID> lecturerIds);
    List<LecturerSubject> findActiveBySubjectId(UUID subjectId);
    List<LecturerSubject> findActiveByLecturerId(UUID lecturerId);
    boolean isLecturerAssignedToSubject(UUID lecturerId, UUID subjectId);
}
