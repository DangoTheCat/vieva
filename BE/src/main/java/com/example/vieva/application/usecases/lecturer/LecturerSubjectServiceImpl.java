package com.example.vieva.application.usecases.lecturer;

import com.example.vieva.application.ports.input.AssignLecturerRequest;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.JsonSerializerPort;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LecturerSubjectServiceImpl implements LecturerSubjectService {

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final JsonSerializerPort jsonSerializer;

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

        if (request.getLecturerIds().stream().anyMatch(Objects::isNull)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Preserve request order while dropping duplicates so each lecturer is validated once.
        List<UUID> lecturerIds = request.getLecturerIds().stream()
                .distinct()
                .collect(Collectors.toList());

        // Two batched look-ups for the whole request instead of two queries per lecturer
        // (Rule 4: database queries executed inside loops / N+1).
        Map<UUID, User> usersById = userRepository.findAllByIds(lecturerIds).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user, (first, second) -> first));
        Set<UUID> alreadyAssigned = lecturerSubjectRepository
                .findActiveAssignments(subject.getSubjectId(), lecturerIds).stream()
                .map(LecturerSubject::getLecturerId)
                .collect(Collectors.toSet());

        List<LecturerSubject> assignmentsToSave = new ArrayList<>(lecturerIds.size());
        List<AuditEvent> auditEventsToSave = new ArrayList<>(lecturerIds.size());

        for (UUID lecturerId : lecturerIds) {
            User user = usersById.get(lecturerId);
            if (user == null) {
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }

            if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.CANNOT_ASSIGN_INACTIVE_USER);
            }

            if (!user.isLecturer() && !user.isAdmin()) {
                throw new AppException(ErrorCode.NOT_A_LECTURER);
            }

            if (alreadyAssigned.contains(lecturerId)) {
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
            assignmentsToSave.add(assignment);

            Map<String, Object> auditValues = new java.util.HashMap<>();
            auditValues.put("subjectId", subject.getSubjectId() != null ? subject.getSubjectId().toString() : null);
            auditValues.put("lecturerId", lecturerId != null ? lecturerId.toString() : null);

            auditEventsToSave.add(AuditEvent.builder()
                    .auditId(UUID.randomUUID())
                    .actorId(currentAdminId)
                    .actionType("LECTURER_ASSIGNED")
                    .entityType("LECTURER_SUBJECT")
                    .entityId(assignment.getLecturerSubjectId().toString())
                    .newValuesJson(jsonSerializer.serialize(auditValues))
                    .createdAt(Instant.now())
                    .build());
        }

        // Batched persistence: 2 round-trips total instead of 2 per lecturer (Rule 4).
        List<LecturerSubject> assignedList = lecturerSubjectRepository.saveAll(assignmentsToSave);
        auditEventRepository.saveAll(auditEventsToSave);

        return assignedList;
    }

    @Override
    public void revokeAssignment(UUID assignmentId, UUID currentAdminId) {
        LecturerSubject assignment = lecturerSubjectRepository.findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        if (assignment.isRevoked()) {
            throw new AppException(ErrorCode.ASSIGNMENT_NOT_FOUND);
        }

        assignment.revoke();
        lecturerSubjectRepository.save(assignment);

        Map<String, Object> auditValues = new java.util.HashMap<>();
        auditValues.put("isActive", false);

        auditEventRepository.save(AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actorId(currentAdminId)
                .actionType("LECTURER_ASSIGNMENT_REVOKED")
                .entityType("LECTURER_SUBJECT")
                .entityId(assignment.getLecturerSubjectId().toString())
                .newValuesJson(jsonSerializer.serialize(auditValues))
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
