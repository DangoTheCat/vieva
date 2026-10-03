package com.example.vieva.application.usecases.lecturer;

import com.example.vieva.application.ports.input.AssignLecturerRequest;
import com.example.vieva.domain.entities.LecturerSubject;

import java.util.List;
import java.util.UUID;

public interface LecturerSubjectService {
    List<LecturerSubject> assignLecturersToSubject(AssignLecturerRequest request, UUID currentAdminId);
    void revokeAssignment(UUID assignmentId, UUID currentAdminId);
    List<LecturerSubject> getLecturersBySubject(UUID subjectId);
    List<LecturerSubject> getSubjectsByLecturer(UUID lecturerId);
    boolean isLecturerAssignedToSubject(UUID lecturerId, UUID subjectId);
}
