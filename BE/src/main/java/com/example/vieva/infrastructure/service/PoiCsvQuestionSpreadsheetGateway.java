package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.ImportRow;
import com.example.vieva.application.ports.output.QuestionSpreadsheetPort;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Excel (Apache POI) and CSV (Commons CSV, UTF-8 with optional BOM) reader/writer for UC1.6.
 * Only the first sheet is read; cell values are taken as displayed text.
 */
@Component
public class PoiCsvQuestionSpreadsheetGateway implements QuestionSpreadsheetPort {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Override
    public List<ImportRow> read(byte[] content, String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        try {
            return lower.endsWith(".csv") ? readCsv(content) : readXlsx(content);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID, "The file cannot be read as "
                    + (lower.endsWith(".csv") ? "CSV (UTF-8)" : "an Excel .xlsx workbook"));
        }
    }

    private List<ImportRow> readXlsx(byte[] content) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new AppException(ErrorCode.IMPORT_FILE_INVALID, "The workbook has no sheet");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new AppException(ErrorCode.IMPORT_FILE_INVALID, "The header row is missing");
            }
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                headers.add(normalizeHeader(formatter.formatCellValue(headerRow.getCell(c))));
            }
            List<ImportRow> rows = new ArrayList<>();
            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                Map<String, String> values = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    if (headers.get(c).isEmpty()) {
                        continue;
                    }
                    Cell cell = row == null ? null : row.getCell(c);
                    values.put(headers.get(c), cell == null ? "" : formatter.formatCellValue(cell));
                }
                rows.add(new ImportRow(r + 1, values));
            }
            return rows;
        }
    }

    private List<ImportRow> readCsv(byte[] content) throws IOException {
        int offset = startsWithBom(content) ? UTF8_BOM.length : 0;
        try (Reader reader = new InputStreamReader(
                new ByteArrayInputStream(content, offset, content.length - offset), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(false)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            List<String> headers = parser.getHeaderNames();
            List<ImportRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String header = normalizeHeader(headers.get(i));
                    if (!header.isEmpty()) {
                        values.put(header, i < record.size() ? record.get(i) : "");
                    }
                }
                // Header is line 1; data lines are numbered like a spreadsheet.
                rows.add(new ImportRow((int) record.getRecordNumber() + 1, values));
            }
            return rows;
        }
    }

    @Override
    public byte[] template(String format, List<String> headers, List<List<String>> sampleRows) {
        try {
            return "csv".equals(format) ? csvTemplate(headers, sampleRows) : xlsxTemplate(headers, sampleRows);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot build import template", e);
        }
    }

    private byte[] xlsxTemplate(List<String> headers, List<List<String>> sampleRows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("questions");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            Row header = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headerStyle);
            }
            for (int r = 0; r < sampleRows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> values = sampleRows.get(r);
                for (int c = 0; c < values.size(); c++) {
                    row.createCell(c).setCellValue(values.get(c));
                }
            }
            for (int c = 0; c < headers.size(); c++) {
                sheet.setColumnWidth(c, 30 * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] csvTemplate(List<String> headers, List<List<String>> sampleRows) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // BOM so Excel opens the UTF-8 file with Vietnamese characters intact.
        out.write(UTF8_BOM);
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), CSVFormat.DEFAULT)) {
            printer.printRecord(headers);
            for (List<String> row : sampleRows) {
                printer.printRecord(row);
            }
        }
        return out.toByteArray();
    }

    private static boolean startsWithBom(byte[] content) {
        return content.length >= 3 && content[0] == UTF8_BOM[0] && content[1] == UTF8_BOM[1] && content[2] == UTF8_BOM[2];
    }

    private static String normalizeHeader(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
