package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.ImportTextQuestionsApiRequest;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.importing.ImportReport;
import com.example.vieva.application.ports.input.importing.QuestionImportService;
import com.example.vieva.application.ports.input.importing.TextQuestionImportCommand;
import com.example.vieva.application.ports.input.importing.TextQuestionImportResult;
import com.example.vieva.application.ports.input.importing.TextQuestionImportService;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
    private final TextQuestionImportService textQuestionImportService;

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
                                                        @AuthenticationPrincipal ActorView currentUser) {
        UUID userId = CurrentUser.id(currentUser);
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(ErrorCode.IMPORT_FILE_INVALID, "The file is empty");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new ApplicationException(ErrorCode.IMPORT_LIMIT_EXCEEDED, "The file exceeds 5120 KB");
        }
        try {
            return ResponseEntity.ok(importService.importQuestions(subjectId, file.getOriginalFilename(),
                    file.getBytes(), dryRun, userId));
        } catch (IOException e) {
            throw new ApplicationException(ErrorCode.IMPORT_FILE_INVALID, "Failed to read uploaded file");
        }
    }

    @PostMapping("/subjects/{subjectId}/questions/import-text")
    @PreAuthorize("@courseSecurityEvaluator.canAccessSubject(#subjectId, authentication)")
    public ResponseEntity<TextQuestionImportResult> importQuestionsFromText(
            @PathVariable UUID subjectId,
            @Valid @RequestBody ImportTextQuestionsApiRequest request,
            @AuthenticationPrincipal ActorView currentUser) {
        UUID userId = CurrentUser.id(currentUser);
        TextQuestionImportCommand cmd = TextQuestionImportCommand.builder()
                .subjectId(subjectId)
                .text(request.getText())
                .topicId(request.getTopicId())
                .bloomLevel(request.getBloomLevel())
                .rubricTemplateId(request.getRubricTemplateId())
                .build();
        return ResponseEntity.ok(textQuestionImportService.importTextQuestions(cmd, userId));
    }
}
