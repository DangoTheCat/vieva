package com.example.vieva.application.ports.output;

public interface FileStoragePort {
    String uploadFile(String filename, byte[] content, String contentType);
}
