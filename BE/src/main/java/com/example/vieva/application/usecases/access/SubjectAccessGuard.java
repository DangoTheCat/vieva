package com.example.vieva.application.usecases.access;

import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * BR-06: course-scoped authorization re-checked inside every write transaction, so a lecturer
 * whose assignment was revoked while the screen was still open cannot publish anything.
 * Administrators keep global access, consistent with the HTTP-level evaluator.
 */
@Component
@RequiredArgsConstructor
public class SubjectAccessGuard {

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final UserRepository userRepository;

    public void requireManage(UUID actorId, UUID subjectId) {
        if (actorId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        if (subjectId == null) {
            throw new AppException(ErrorCode.FORBIDDEN_SUBJECT);
        }
        if (lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId)) {
            return;
        }
        boolean activeAdmin = userRepository.findById(actorId)
                .filter(user -> !user.isDeleted() && user.getStatus() == UserStatus.ACTIVE)
                .map(User::isAdmin)
                .orElse(false);
        if (!activeAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN_SUBJECT);
        }
    }
}
