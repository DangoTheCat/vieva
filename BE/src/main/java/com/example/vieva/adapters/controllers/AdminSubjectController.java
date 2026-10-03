package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.CreateSubjectApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateSubjectApiRequest;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.adapters.presenters.PageResponse;
import com.example.vieva.adapters.presenters.SubjectDto;
import com.example.vieva.adapters.presenters.SubjectPresenter;
import com.example.vieva.application.ports.input.CreateSubjectRequest;
import com.example.vieva.application.ports.input.SubjectSearchCriteria;
import com.example.vieva.application.ports.input.UpdateSubjectRequest;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.application.usecases.subject.SubjectService;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subjects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubjectController {

    private final SubjectService subjectService;
    private final SubjectPresenter subjectPresenter;

    @GetMapping
    public ResponseEntity<PageResponse<SubjectDto>> getSubjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SubjectStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        SubjectSearchCriteria criteria = SubjectSearchCriteria.builder()
                .keyword(keyword)
                .status(status)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PagedResult<Subject> pagedSubjects = subjectService.getSubjects(criteria);
        return ResponseEntity.ok(subjectPresenter.toPageResponse(pagedSubjects));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectDto> getSubjectById(@PathVariable UUID id) {
        Subject subject = subjectService.getSubjectById(id);
        return ResponseEntity.ok(subjectPresenter.toDto(subject));
    }

    @PostMapping
    public ResponseEntity<SubjectDto> createSubject(
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody CreateSubjectApiRequest request) {

        UUID adminId = resolveAdminId(currentAdmin);

        CreateSubjectRequest createRequest = CreateSubjectRequest.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .description(request.getDescription())
                .credits(request.getCredits())
                .status(request.getStatus())
                .build();

        Subject created = subjectService.createSubject(createRequest, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectPresenter.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectDto> updateSubject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin,
            @Valid @RequestBody UpdateSubjectApiRequest request) {

        UUID adminId = resolveAdminId(currentAdmin);

        UpdateSubjectRequest updateRequest = UpdateSubjectRequest.builder()
                .subjectName(request.getSubjectName())
                .description(request.getDescription())
                .credits(request.getCredits())
                .status(request.getStatus())
                .build();

        Subject updated = subjectService.updateSubject(id, updateRequest, adminId);
        return ResponseEntity.ok(subjectPresenter.toDto(updated));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<MessageResponse> deactivateSubject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin) {

        UUID adminId = resolveAdminId(currentAdmin);
        subjectService.deactivateSubject(id, adminId);
        return ResponseEntity.ok(new MessageResponse("Subject deactivated successfully"));
    }

    private UUID resolveAdminId(User currentAdmin) {
        if (currentAdmin == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return currentAdmin.getUserId();
    }
}
