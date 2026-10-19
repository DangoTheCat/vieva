package com.example.vieva.application.usecases.lecturer;

import com.example.vieva.application.ports.input.lecturer.AssignLecturerRequest;
import com.example.vieva.application.ports.input.lecturer.LecturerSubjectResult;
import com.example.vieva.application.ports.input.lecturer.LecturerSubjectService;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.domain.entities.AuditEvent;
import com.example.vieva.domain.entities.LecturerSubject;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.application.transaction.UseCaseTransaction;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@UseCaseTransaction
public class LecturerSubjectServiceImpl implements LecturerSubjectService {

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final JsonSerializerPort jsonSerializer;
    private final QuestionBankAuditor auditor;

    @Override
    public List<LecturerSubjectResult> assignLecturersToSubject(AssignLecturerRequest request, UUID currentAdminId) {
        if (request.getSubjectId() == null || request.getLecturerIds() == null || request.getLecturerIds().isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_NOT_FOUND));

        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.CANNOT_ASSIGN_INACTIVE_SUBJECT);
        }

        if (request.getLecturerIds().stream().anyMatch(Objects::isNull)) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
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
                throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
            }

            if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
                throw new ApplicationException(ErrorCode.CANNOT_ASSIGN_INACTIVE_USER);
            }

            if (!user.isLecturer() && !user.isAdmin()) {
                throw new ApplicationException(ErrorCode.NOT_A_LECTURER);
            }

            if (alreadyAssigned.contains(lecturerId)) {
                throw new ApplicationException(ErrorCode.CANNOT_ASSIGN_DUPLICATE);
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
        final List<LecturerSubject> assignedList;
        try {
            assignedList = lecturerSubjectRepository.saveAll(assignmentsToSave);
            auditEventRepository.saveAll(auditEventsToSave);
        } catch (DuplicateKeyException e) {
            // Concurrent assign won the race (partial unique index uq_lecturer_subject)
            throw new ApplicationException(ErrorCode.CANNOT_ASSIGN_DUPLICATE);
        }

        return assignedList.stream().map(LecturerSubjectResult::from).toList();
    }

    @Override
    public void revokeAssignment(UUID assignmentId, UUID currentAdminId) {
        LecturerSubject assignment = lecturerSubjectRepository.findById(assignmentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        if (assignment.isRevoked()) {
            throw new ApplicationException(ErrorCode.ASSIGNMENT_NOT_FOUND);
        }

        assignment.revoke();
        lecturerSubjectRepository.save(assignment);

        Map<String, Object> auditValues = new java.util.HashMap<>();
        auditValues.put("isActive", false);

        auditor.record(currentAdminId, "LECTURER_ASSIGNMENT_REVOKED", "LECTURER_SUBJECT",
                assignment.getLecturerSubjectId(), auditValues);
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<LecturerSubjectResult> getLecturersBySubject(UUID subjectId) {
        return lecturerSubjectRepository.findActiveBySubjectId(subjectId).stream()
                .map(LecturerSubjectResult::from)
                .toList();
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<LecturerSubjectResult> getSubjectsByLecturer(UUID lecturerId) {
        return lecturerSubjectRepository.findActiveByLecturerId(lecturerId).stream()
                .map(LecturerSubjectResult::from)
                .toList();
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public boolean isLecturerAssignedToSubject(UUID lecturerId, UUID subjectId) {
        return lecturerSubjectRepository.isLecturerAssignedToSubject(lecturerId, subjectId);
    }
}
