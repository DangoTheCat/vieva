package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.presenters.CourseDocumentDto;
import com.example.vieva.adapters.presenters.CourseDocumentPresenter;
import com.example.vieva.adapters.presenters.MessageResponse;
import com.example.vieva.application.usecases.document.DocumentIndexingService;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * UC1.1: upload (202 — indexing runs asynchronously), status polling, retry of FAILED indexing.
 */
@RestController
@RequestMapping("/api/v1/lecturer")
@RequiredArgsConstructor
public class LecturerDocumentController {

    private final DocumentIndexingService documentIndexingService;
    private final CourseDocumentPresenter presenter;

    @PostMapping(value = "/subjects/{subjectId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<CourseDocumentDto> uploadDocument(
            @PathVariable UUID subjectId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        UUID userId = CurrentUser.id(currentUser);
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Uploaded file must not be empty");
        }
        try {
            CourseDocument document = documentIndexingService.uploadDocument(
                    subjectId, file.getOriginalFilename(), file.getBytes(), userId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(presenter.toDto(document));
        } catch (IOException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Failed to read uploaded file");
        }
    }

    @GetMapping("/subjects/{subjectId}/documents")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<List<CourseDocumentDto>> getDocumentsBySubject(@PathVariable UUID subjectId) {
        return ResponseEntity.ok(presenter.toDtoList(documentIndexingService.getDocumentsBySubject(subjectId)));
    }

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessDocument(#documentId, authentication)")
    public ResponseEntity<CourseDocumentDto> getDocumentById(@PathVariable UUID documentId) {
        return ResponseEntity.ok(presenter.toDto(documentIndexingService.getDocumentById(documentId)));
    }

    @PostMapping("/documents/{documentId}/retry-index")
    @PreAuthorize("@courseSecurityEvaluator.canAccessDocument(#documentId, authentication)")
    public ResponseEntity<CourseDocumentDto> retryIndexing(@PathVariable UUID documentId,
                                                           @AuthenticationPrincipal User currentUser) {
        CourseDocument document = documentIndexingService.retryIndexing(documentId, CurrentUser.id(currentUser));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(presenter.toDto(document));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessDocument(#documentId, authentication)")
    public ResponseEntity<MessageResponse> deleteDocument(@PathVariable UUID documentId,
                                                          @AuthenticationPrincipal User currentUser) {
        documentIndexingService.softDeleteDocument(documentId, CurrentUser.id(currentUser));
        return ResponseEntity.ok(new MessageResponse("Document deleted successfully"));
    }
}
