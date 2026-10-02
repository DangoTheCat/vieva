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

@RestController
@RequestMapping("/api/v1/lecturer")
@RequiredArgsConstructor
public class LecturerDocumentController {

    private final DocumentIndexingService documentIndexingService;
    private final CourseDocumentPresenter presenter;

    @PostMapping(value = "/subjects/{subjectId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId)")
    public ResponseEntity<CourseDocumentDto> uploadDocument(
            @PathVariable UUID subjectId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Uploaded file must not be empty");
        }

        try {
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            byte[] bytes = file.getBytes();

            CourseDocument document = documentIndexingService.uploadDocument(
                    subjectId, originalFilename, bytes, contentType, userId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(presenter.toDto(document));
        } catch (IOException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Failed to read uploaded file");
        }
    }

    @GetMapping("/subjects/{subjectId}/documents")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId)")
    public ResponseEntity<List<CourseDocumentDto>> getDocumentsBySubject(@PathVariable UUID subjectId) {
        List<CourseDocument> documents = documentIndexingService.getDocumentsBySubject(subjectId);
        return ResponseEntity.ok(presenter.toDtoList(documents));
    }

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessDocument(#documentId)")
    public ResponseEntity<CourseDocumentDto> getDocumentById(@PathVariable UUID documentId) {
        CourseDocument document = documentIndexingService.getDocumentById(documentId);
        return ResponseEntity.ok(presenter.toDto(document));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("@courseSecurityEvaluator.canAccessDocument(#documentId)")
    public ResponseEntity<MessageResponse> deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = resolveUserId(currentUser);
        documentIndexingService.softDeleteDocument(documentId, userId);
        return ResponseEntity.ok(new MessageResponse("Document deleted successfully"));
    }

    private UUID resolveUserId(User currentUser) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return currentUser.getUserId();
    }
}
