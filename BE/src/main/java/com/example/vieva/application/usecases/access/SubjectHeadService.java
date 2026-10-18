package com.example.vieva.application.usecases.access;

import com.example.vieva.application.ports.output.*;
import com.example.vieva.domain.entities.*;
import com.example.vieva.domain.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectHeadService {
    private final SubjectRepository subjects;
    private final LecturerSubjectRepository assignments;
    private final UserRepository users;

    @Transactional
    public Subject assign(UUID subjectId, UUID lecturerId, UUID adminId) {
        users.findById(adminId).filter(u -> u.isAdmin() && !u.isDeleted() && u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
        Subject subject = subjects.findByIdForUpdate(subjectId).orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        if (subject.getStatus() != SubjectStatus.ACTIVE) throw new AppException(ErrorCode.SUBJECT_INACTIVE);
        requireActiveLecturer(lecturerId, subjectId);
        subject.setHeadLecturerId(lecturerId);
        return subjects.save(subject);
    }

    @Transactional
    public UUID headOf(UUID subjectId) {
        Subject subject = subjects.findByIdForUpdate(subjectId).orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        if (subject.getHeadLecturerId() == null) throw new AppException(ErrorCode.SUBJECT_HEAD_REQUIRED);
        requireActiveLecturer(subject.getHeadLecturerId(), subjectId);
        return subject.getHeadLecturerId();
    }

    @Transactional
    public void requireHead(UUID actorId, UUID subjectId) {
        if (actorId == null) throw new AppException(ErrorCode.UNAUTHENTICATED);
        if (!actorId.equals(headOf(subjectId))) throw new AppException(ErrorCode.NOT_SUBJECT_HEAD);
    }

    private void requireActiveLecturer(UUID lecturerId, UUID subjectId) {
        boolean active = users.findById(lecturerId)
                .filter(u -> u.isLecturer() && !u.isDeleted() && u.getStatus() == UserStatus.ACTIVE).isPresent();
        if (!active || !assignments.isLecturerAssignedToSubject(lecturerId, subjectId)) {
            throw new AppException(ErrorCode.SUBJECT_HEAD_REQUIRED);
        }
    }
}
