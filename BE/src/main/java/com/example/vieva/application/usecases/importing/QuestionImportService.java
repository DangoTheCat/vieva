package com.example.vieva.application.usecases.importing;

import com.example.vieva.application.ports.output.ImportReport;

import java.util.UUID;

/**
 * UC1.6: bulk import from Excel/CSV. One row per rubric criterion; rows sharing a question_ref form
 * one question. Valid questions become IMPORT DRAFTs; dry-run validates without writing.
 */
public interface QuestionImportService {
    String FORMAT_XLSX = "xlsx";
    String FORMAT_CSV = "csv";

    byte[] template(String format);

    ImportReport importQuestions(UUID subjectId, String filename, byte[] content, boolean dryRun, UUID actorId);
}
