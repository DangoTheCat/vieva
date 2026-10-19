package com.example.vieva.application.usecases.access;

import com.example.vieva.application.ports.input.access.SubjectHeadService;
import com.example.vieva.application.ports.input.subject.SubjectResult;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import com.example.vieva.application.transaction.UseCaseTransaction;

import java.util.UUID;

@RequiredArgsConstructor
public class SubjectHeadServiceImpl implements SubjectHeadService {
    private final SubjectRepository subjects;
    private final LecturerSubjectRepository assignments;
    private final UserRepository users;

    @Override
    @UseCaseTransaction
    public SubjectResult assign(UUID subjectId, UUID lecturerId, UUID adminId) {
        users.findById(adminId).filter(u -> u.isAdmin() && !u.isDeleted() && u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.UNAUTHORIZED));
        Subject subject = subjects.findByIdForUpdate(subjectId).orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_NOT_FOUND));
        if (subject.getStatus() != SubjectStatus.ACTIVE) throw new ApplicationException(ErrorCode.SUBJECT_INACTIVE);
        requireActiveLecturer(lecturerId, subjectId);
        subject.setHeadLecturerId(lecturerId);
        return SubjectResult.from(subjects.save(subject));
    }

    @Override
    @UseCaseTransaction
    public UUID headOf(UUID subjectId) {
        Subject subject = subjects.findByIdForUpdate(subjectId).orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_NOT_FOUND));
        if (subject.getHeadLecturerId() == null) throw new ApplicationException(ErrorCode.SUBJECT_HEAD_REQUIRED);
        requireActiveLecturer(subject.getHeadLecturerId(), subjectId);
        return subject.getHeadLecturerId();
    }

    @Override
    @UseCaseTransaction
    public void requireHead(UUID actorId, UUID subjectId) {
        if (actorId == null) throw new ApplicationException(ErrorCode.UNAUTHENTICATED);
        if (!actorId.equals(headOf(subjectId))) throw new ApplicationException(ErrorCode.NOT_SUBJECT_HEAD);
    }

    private void requireActiveLecturer(UUID lecturerId, UUID subjectId) {
        boolean active = users.findById(lecturerId)
                .filter(u -> u.isLecturer() && !u.isDeleted() && u.getStatus() == UserStatus.ACTIVE).isPresent();
        if (!active || !assignments.isLecturerAssignedToSubject(lecturerId, subjectId)) {
            throw new ApplicationException(ErrorCode.SUBJECT_HEAD_REQUIRED);
        }
    }
}
