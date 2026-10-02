package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.QuestionBankApiRequests.CreateTopicRequest;
import com.example.vieva.adapters.presenters.QuestionBankDtos.TopicDto;
import com.example.vieva.adapters.presenters.QuestionBankPresenter;
import com.example.vieva.adapters.presenters.SubjectDto;
import com.example.vieva.adapters.presenters.SubjectPresenter;
import com.example.vieva.application.usecases.catalog.LecturerCatalogService;
import com.example.vieva.domain.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Subjects assigned to the current lecturer and their topics.
 */
@RestController
@RequestMapping("/api/v1/lecturer")
@RequiredArgsConstructor
public class LecturerCatalogController {

    private final LecturerCatalogService catalogService;
    private final SubjectPresenter subjectPresenter;
    private final QuestionBankPresenter presenter;

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectDto>> listAssignedSubjects(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(subjectPresenter.toDtoList(catalogService.listAssignedSubjects(CurrentUser.id(currentUser))));
    }

    @GetMapping("/subjects/{subjectId}/topics")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<List<TopicDto>> listTopics(@PathVariable UUID subjectId) {
        return ResponseEntity.ok(catalogService.listTopics(subjectId).stream().map(presenter::toTopic).toList());
    }

    @PostMapping("/subjects/{subjectId}/topics")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<TopicDto> createTopic(@PathVariable UUID subjectId,
                                                @Valid @RequestBody CreateTopicRequest request,
                                                @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presenter.toTopic(
                catalogService.createTopic(subjectId, request.name(), request.description(), CurrentUser.id(currentUser))));
    }
}
