package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.output.ImportReport;
import com.example.vieva.application.usecases.importing.QuestionImportService;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * UC1.6: template download and import (dry-run validation or commit as IMPORT drafts).
 */
@RestController
@RequestMapping("/api/v1/lecturer")
@RequiredArgsConstructor
public class LecturerQuestionImportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final QuestionImportService importService;

    @GetMapping("/import-templates/questions")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam(defaultValue = "xlsx") String format) {
        String normalized = format.toLowerCase(Locale.ROOT);
        byte[] content = importService.template(normalized);
        boolean csv = QuestionImportService.FORMAT_CSV.equals(normalized);
        return ResponseEntity.ok()
                .contentType(csv ? new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8) : XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("question-import-template." + (csv ? "csv" : "xlsx"))
                        .build()
                        .toString())
                .body(content);
    }

    @PostMapping(value = "/subjects/{subjectId}/questions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<ImportReport> importQuestions(@PathVariable UUID subjectId,
                                                        @RequestParam("file") MultipartFile file,
                                                        @RequestParam(defaultValue = "true") boolean dryRun,
                                                        @AuthenticationPrincipal User currentUser) {
        UUID userId = CurrentUser.id(currentUser);
        try {
            return ResponseEntity.ok(importService.importQuestions(subjectId, file.getOriginalFilename(),
                    file.getBytes(), dryRun, userId));
        } catch (IOException e) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID, "Failed to read uploaded file");
        }
    }
}
