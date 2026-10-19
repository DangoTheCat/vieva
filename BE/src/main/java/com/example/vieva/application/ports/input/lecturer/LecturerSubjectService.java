package com.example.vieva.application.ports.input.lecturer;

import java.util.List;
import java.util.UUID;

public interface LecturerSubjectService {
    List<LecturerSubjectResult> assignLecturersToSubject(AssignLecturerRequest request, UUID currentAdminId);
    void revokeAssignment(UUID assignmentId, UUID currentAdminId);
    List<LecturerSubjectResult> getLecturersBySubject(UUID subjectId);
    List<LecturerSubjectResult> getSubjectsByLecturer(UUID lecturerId);
    boolean isLecturerAssignedToSubject(UUID lecturerId, UUID subjectId);
}
