package com.example.vieva.domain.services;

import java.util.Optional;

/**
 * Decides whether extracted text from a document is usable,
 * so scanned PDFs fail with a clear, helpful reason instead of silently.
 */
public final class ExtractionCheck {

    static final int MIN_CHARS_PER_PAGE = 80;
    static final double MIN_TEXT_PAGE_SHARE = 0.5;

    private ExtractionCheck() {
    }

    public static Optional<String> checkProblem(String extension, Integer pageCount, int characters, int sectionCount) {
        if (characters == 0) {
            return Optional.of("pdf".equalsIgnoreCase(extension)
                    ? "Tài liệu PDF này không có lớp văn bản (dạng scan ảnh). Vui lòng chạy OCR hoặc xuất dạng PDF có chữ rồi tải lên lại."
                    : "Tệp tài liệu không chứa bất kỳ nội dung văn bản nào.");
        }
        if (!"pdf".equalsIgnoreCase(extension) || pageCount == null || pageCount <= 1) {
            return Optional.empty();
        }
        boolean sparse = (double) characters / pageCount < MIN_CHARS_PER_PAGE;
        boolean fewTextPages = sectionCount > 1 && (double) sectionCount / pageCount < MIN_TEXT_PAGE_SHARE;
        if (sparse || fewTextPages) {
            return Optional.of("Chỉ tìm thấy văn bản trên " + sectionCount + " / " + pageCount + " trang ("
                    + characters + " ký tự). Tài liệu này có thể là dạng scan ảnh thiếu chữ; vui lòng kiểm tra lại.");
        }
        return Optional.empty();
    }
}
