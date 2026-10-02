package com.example.vieva.application.ports.output;

/**
 * Detects the real MIME type from the file content (magic bytes), never trusting the client header.
 */
public interface FileTypeDetectorPort {
    String detect(byte[] content, String filename);
}
