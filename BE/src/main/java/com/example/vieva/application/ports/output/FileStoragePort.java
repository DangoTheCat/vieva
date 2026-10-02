package com.example.vieva.application.ports.output;

/**
 * Stores raw uploaded files. {@link #load} must return the exact bytes so a FAILED document
 * can be re-indexed without another upload.
 */
public interface FileStoragePort {
    StoredFile store(String filename, byte[] content, String contentType);

    byte[] load(String storageKey);
}
