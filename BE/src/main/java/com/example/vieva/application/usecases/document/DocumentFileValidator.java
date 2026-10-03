package com.example.vieva.application.usecases.document;

import com.example.vieva.application.settings.DocumentSettings;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Upload checks for UC1.1: file name sanitizing, size limit, extension whitelist and consistency
 * between the extension and the MIME type detected from the content.
 */
public final class DocumentFileValidator {

    public static final Map<String, Set<String>> MIME_BY_EXTENSION = Map.of(
            "pdf", Set.of("application/pdf"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            "txt", Set.of("text/plain"));

    private static final int MAX_NAME_LENGTH = 200;

    private DocumentFileValidator() {
    }

    /** Strips any path, control and unsafe characters; keeps letters (incl. Vietnamese), digits, space . _ - ( ). */
    public static String sanitizeFilename(String original) {
        String name = original == null ? "" : original;
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = Normalizer.normalize(name, Normalizer.Form.NFC)
                .replaceAll("[^\\p{L}\\p{N} ._()\\-]", "_")
                .replaceAll("_+", "_")
                .trim();
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        if (name.isBlank()) {
            name = "document";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            String extension = extensionOf(name);
            int maxBase = Math.max(0, MAX_NAME_LENGTH - extension.length() - 1);
            String base = name.substring(0, maxBase);
            // Fall back to a hard truncate when there is no usable base
            // (e.g. an extremely long extension would otherwise yield a negative index).
            name = extension.isEmpty() || base.isEmpty()
                    ? name.substring(0, MAX_NAME_LENGTH)
                    : base + "." + extension;
        }
        return name;
    }

    public static String extensionOf(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 || dot == filename.length() - 1 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static void validateSize(byte[] content, DocumentSettings settings) {
        if (content == null || content.length == 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Uploaded file is empty");
        }
        if (content.length > settings.getMaxSizeBytes()) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE,
                    "File exceeds the maximum size of " + (settings.getMaxSizeBytes() / (1024 * 1024)) + " MB");
        }
    }

    /**
     * @return the canonical MIME type stored with the document
     */
    public static String validateType(String filename, String detectedMime, DocumentSettings settings) {
        String extension = extensionOf(filename);
        boolean allowed = settings.getAllowedExtensions().stream()
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
        Set<String> expected = MIME_BY_EXTENSION.get(extension);
        if (!allowed || expected == null) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "Allowed file types: " + String.join(", ", settings.getAllowedExtensions()));
        }
        String mime = detectedMime == null ? "" : detectedMime.toLowerCase(Locale.ROOT);
        int parameters = mime.indexOf(';');
        if (parameters >= 0) {
            mime = mime.substring(0, parameters).trim();
        }
        if (!expected.contains(mime)) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "File content (" + (mime.isEmpty() ? "unknown" : mime) + ") does not match the ." + extension + " extension");
        }
        return mime;
    }
}
