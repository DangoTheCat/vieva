package com.example.vieva.application.ports.output;

import java.util.List;

/**
 * Reads/writes the question import file (UC1.6). Formats: xlsx and csv.
 */
public interface QuestionSpreadsheetPort {
    /**
     * @return data rows keyed by lower-case header name; row numbers are 1-based as displayed in Excel
     *         (the header is row 1). Throws AppException IMPORT_FILE_INVALID when the file is unreadable.
     */
    List<ImportRow> read(byte[] content, String filename);

    byte[] template(String format, List<String> headers, List<List<String>> sampleRows);
}
