package com.example.vieva.application.usecases.access;

import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

/**
 * BR-06: course-scoped authorization re-checked inside every write transaction, so a lecturer
 * whose assignment was revoked while the screen was still open cannot publish anything.
 * Verifies active actor and role before trusting any lecturer subject assignment.
 * Administrators keep global access.
 */
@RequiredArgsConstructor
public class SubjectAccessGuard {

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final UserRepository userRepository;

    public boolean canManage(UUID actorId, UUID subjectId) {
        if (actorId == null || subjectId == null) {
            return false;
        }

        Optional<User> userOpt = userRepository.findById(actorId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
            return false;
        }

        if (user.isAdmin()) {
            return true;
        }

        if (user.isLecturer()) {
            return lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId);
        }

        return false;
    }

    public void requireManage(UUID actorId, UUID subjectId) {
        if (actorId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHENTICATED);
        }
        if (!canManage(actorId, subjectId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_SUBJECT);
        }
    }
}
