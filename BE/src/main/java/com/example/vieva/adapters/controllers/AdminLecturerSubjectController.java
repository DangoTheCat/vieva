package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.AssignLecturerApiRequest;
import com.example.vieva.adapters.presenters.LecturerSubjectDto;
import com.example.vieva.adapters.presenters.LecturerSubjectPresenter;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.application.ports.input.AssignLecturerRequest;
import com.example.vieva.application.usecases.lecturer.LecturerSubjectService;
import com.example.vieva.domain.entities.LecturerSubject;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/lecturer-subjects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLecturerSubjectController {

    private final LecturerSubjectService lecturerSubjectService;
    private final LecturerSubjectPresenter presenter;

    @PostMapping
    public ResponseEntity<List<LecturerSubjectDto>> assignLecturers(
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody AssignLecturerApiRequest apiRequest) {

        UUID adminId = resolveAdminId(currentAdmin);

        AssignLecturerRequest request = AssignLecturerRequest.builder()
                .subjectId(apiRequest.getSubjectId())
                .lecturerIds(apiRequest.getLecturerIds())
                .build();

        List<LecturerSubject> assignments = lecturerSubjectService.assignLecturersToSubject(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.toDtoList(assignments));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> revokeAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin) {

        UUID adminId = resolveAdminId(currentAdmin);
        lecturerSubjectService.revokeAssignment(id, adminId);
        return ResponseEntity.ok(new MessageResponse("Lecturer assignment revoked successfully"));
    }

    @GetMapping("/by-subject/{subjectId}")
    public ResponseEntity<List<LecturerSubjectDto>> getLecturersBySubject(@PathVariable UUID subjectId) {
        List<LecturerSubject> assignments = lecturerSubjectService.getLecturersBySubject(subjectId);
        return ResponseEntity.ok(presenter.toDtoList(assignments));
    }

    @GetMapping("/by-lecturer/{lecturerId}")
    public ResponseEntity<List<LecturerSubjectDto>> getSubjectsByLecturer(@PathVariable UUID lecturerId) {
        List<LecturerSubject> assignments = lecturerSubjectService.getSubjectsByLecturer(lecturerId);
        return ResponseEntity.ok(presenter.toDtoList(assignments));
    }

    private UUID resolveAdminId(User currentAdmin) {
        if (currentAdmin == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return currentAdmin.getUserId();
    }
}
