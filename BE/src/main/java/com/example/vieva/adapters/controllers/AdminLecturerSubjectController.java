package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.AssignLecturerApiRequest;
import com.example.vieva.adapters.presenters.LecturerSubjectDto;
import com.example.vieva.adapters.presenters.LecturerSubjectPresenter;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.lecturer.AssignLecturerRequest;
import com.example.vieva.application.ports.input.lecturer.LecturerSubjectResult;
import com.example.vieva.application.ports.input.lecturer.LecturerSubjectService;
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
            @AuthenticationPrincipal ActorView currentAdmin,
            @Valid @RequestBody AssignLecturerApiRequest apiRequest) {

        UUID adminId = resolveAdminId(currentAdmin);

        AssignLecturerRequest request = AssignLecturerRequest.builder()
                .subjectId(apiRequest.getSubjectId())
                .lecturerIds(apiRequest.getLecturerIds())
                .build();

        List<LecturerSubjectResult> assignments = lecturerSubjectService.assignLecturersToSubject(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.toDtoList(assignments));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> revokeAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal ActorView currentAdmin) {

        UUID adminId = resolveAdminId(currentAdmin);
        lecturerSubjectService.revokeAssignment(id, adminId);
        return ResponseEntity.ok(new MessageResponse("Lecturer assignment revoked successfully"));
    }

    @GetMapping("/by-subject/{subjectId}")
    public ResponseEntity<List<LecturerSubjectDto>> getLecturersBySubject(@PathVariable UUID subjectId) {
        List<LecturerSubjectResult> assignments = lecturerSubjectService.getLecturersBySubject(subjectId);
        return ResponseEntity.ok(presenter.toDtoList(assignments));
    }

    @GetMapping("/by-lecturer/{lecturerId}")
    public ResponseEntity<List<LecturerSubjectDto>> getSubjectsByLecturer(@PathVariable UUID lecturerId) {
        List<LecturerSubjectResult> assignments = lecturerSubjectService.getSubjectsByLecturer(lecturerId);
        return ResponseEntity.ok(presenter.toDtoList(assignments));
    }

    private UUID resolveAdminId(ActorView currentAdmin) {
        return CurrentUser.id(currentAdmin);
    }
}
