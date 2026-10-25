package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.question.*;
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
@RequestMapping("/api/v1/lecturer/subjects/{subjectId}/rubric-templates")
@RequiredArgsConstructor
public class LecturerRubricTemplateController {

    private final RubricTemplateService rubricTemplateService;

    public record CreateTemplateApiRequest(
            String name,
            String description,
            String criteriaJson
    ) {}

    public record UpdateTemplateApiRequest(
            String name,
            String description,
            String criteriaJson
    ) {}

    @GetMapping
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<List<RubricTemplateResult>> listTemplates(@PathVariable UUID subjectId) {
        return ResponseEntity.ok(rubricTemplateService.getTemplatesBySubject(subjectId));
    }

    @PostMapping
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<RubricTemplateResult> createTemplate(
            @PathVariable UUID subjectId,
            @Valid @RequestBody CreateTemplateApiRequest request,
            @AuthenticationPrincipal ActorView currentUser
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        CreateRubricTemplateCommand cmd = CreateRubricTemplateCommand.builder()
                .subjectId(subjectId)
                .name(request.name())
                .description(request.description())
                .criteriaJson(request.criteriaJson())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(rubricTemplateService.createTemplate(cmd, actorId));
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<RubricTemplateResult> getTemplate(
            @PathVariable UUID subjectId,
            @PathVariable UUID templateId
    ) {
        return ResponseEntity.ok(rubricTemplateService.getTemplateById(templateId));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<RubricTemplateResult> updateTemplate(
            @PathVariable UUID subjectId,
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTemplateApiRequest request,
            @AuthenticationPrincipal ActorView currentUser
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        UpdateRubricTemplateCommand cmd = UpdateRubricTemplateCommand.builder()
                .name(request.name())
                .description(request.description())
                .criteriaJson(request.criteriaJson())
                .build();
        return ResponseEntity.ok(rubricTemplateService.updateTemplate(templateId, cmd, actorId));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID subjectId,
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ActorView currentUser
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        rubricTemplateService.deleteTemplate(templateId, actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/copy-to-version/{versionId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<RubricResult> copyToVersion(
            @PathVariable UUID subjectId,
            @PathVariable UUID templateId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal ActorView currentUser
    ) {
        UUID actorId = CurrentUser.id(currentUser);
        return ResponseEntity.ok(rubricTemplateService.copyTemplateToQuestionVersion(templateId, versionId, actorId));
    }
}
