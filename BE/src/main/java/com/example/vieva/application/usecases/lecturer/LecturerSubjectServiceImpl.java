package com.example.vieva.application.usecases.lecturer;

import com.example.vieva.application.ports.input.AssignLecturerRequest;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.AuditEvent;
import com.example.vieva.domain.entities.LecturerSubject;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LecturerSubjectServiceImpl implements LecturerSubjectService {

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;

    @Override
    public List<LecturerSubject> assignLecturersToSubject(AssignLecturerRequest request, UUID currentAdminId) {
        if (request.getSubjectId() == null || request.getLecturerIds() == null || request.getLecturerIds().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new AppException(ErrorCode.CANNOT_ASSIGN_INACTIVE_SUBJECT);
        }

        List<LecturerSubject> assignedList = new ArrayList<>();

        for (UUID lecturerId : request.getLecturerIds()) {
            User user = userRepository.findById(lecturerId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.CANNOT_ASSIGN_INACTIVE_USER);
            }

            if (!user.isLecturer() && !user.isAdmin()) {
                throw new AppException(ErrorCode.NOT_A_LECTURER);
            }

            if (lecturerSubjectRepository.findActiveAssignment(lecturerId, subject.getSubjectId()).isPresent()) {
                throw new AppException(ErrorCode.CANNOT_ASSIGN_DUPLICATE);
            }

            LecturerSubject assignment = LecturerSubject.builder()
                    .lecturerSubjectId(UUID.randomUUID())
                    .lecturerId(lecturerId)
                    .subjectId(subject.getSubjectId())
                    .isActive(true)
                    .assignedAt(Instant.now())
                    .assignedBy(currentAdminId)
                    .build();

            LecturerSubject saved = lecturerSubjectRepository.save(assignment);
            assignedList.add(saved);

            auditEventRepository.save(AuditEvent.builder()
                    .auditId(UUID.randomUUID())
                    .actorId(currentAdminId)
                    .actionType("LECTURER_ASSIGNED")
                    .entityType("LECTURER_SUBJECT")
                    .entityId(saved.getLecturerSubjectId().toString())
                    .newValuesJson(String.format("{\"subjectId\":\"%s\",\"lecturerId\":\"%s\"}",
                            subject.getSubjectId(), lecturerId))
                    .createdAt(Instant.now())
                    .build());
        }

        return assignedList;
    }

    @Override
    public void revokeAssignment(UUID assignmentId, UUID currentAdminId) {
        LecturerSubject assignment = lecturerSubjectRepository.findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        assignment.setIsActive(false);
        assignment.setRevokedAt(Instant.now());
        lecturerSubjectRepository.save(assignment);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("LECTURER_ASSIGNMENT_REVOKED")
                .entityType("LECTURER_SUBJECT")
                .entityId(assignment.getLecturerSubjectId().toString())
                .newValuesJson("{\"isActive\":false}")
                .createdAt(Instant.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LecturerSubject> getLecturersBySubject(UUID subjectId) {
        return lecturerSubjectRepository.findActiveBySubjectId(subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LecturerSubject> getSubjectsByLecturer(UUID lecturerId) {
        return lecturerSubjectRepository.findActiveByLecturerId(lecturerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLecturerAssignedToSubject(UUID lecturerId, UUID subjectId) {
        return lecturerSubjectRepository.isLecturerAssignedToSubject(lecturerId, subjectId);
    }
}
