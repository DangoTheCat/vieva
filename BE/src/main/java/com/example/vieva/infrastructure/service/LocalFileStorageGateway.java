package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.FileStoragePort;
import com.example.vieva.application.ports.output.StoredFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Default storage: files under {@code vieva.storage.local.root}. The key is a relative path
 * generated server-side (never derived from user input beyond the sanitized extension).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vieva.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageGateway implements FileStoragePort {

    private final Path root;

    public LocalFileStorageGateway(@Value("${vieva.storage.local.root:./data/documents}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(String filename, byte[] content, String contentType) {
        String extension = filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).replaceAll("[^A-Za-z0-9]", "")
                : "bin";
        LocalDate today = LocalDate.now();
        String key = today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/"
                + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store uploaded file", e);
        }
        return new StoredFile(key, "local:" + key);
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException("Stored file is missing or unreadable", e);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
