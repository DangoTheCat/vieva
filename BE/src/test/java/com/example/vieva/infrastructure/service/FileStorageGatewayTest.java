package com.example.vieva.infrastructure.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import com.example.vieva.application.ports.output.StoredFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageGatewayTest {

    @Test
    @DisplayName("LocalFileStorageGateway: store creates file, delete removes actual file and preserves unrelated files")
    void localFileStorageStoreAndDeletePathSafe(@TempDir Path tempDir) throws IOException {
        LocalFileStorageGateway gateway = new LocalFileStorageGateway(tempDir.toString());

        // Create an unrelated file in the root
        Path unrelatedFile = tempDir.resolve("unrelated_keep.txt");
        Files.writeString(unrelatedFile, "unrelated content", StandardCharsets.UTF_8);

        // Store a file
        byte[] content = "Knowledge text content".getBytes(StandardCharsets.UTF_8);
        StoredFile stored = gateway.store("knowledge.txt", content, "text/plain");

        assertThat(stored).isNotNull();
        assertThat(stored.storageKey()).isNotNull();

        // Verify file exists on disk
        byte[] loaded = gateway.load(stored.storageKey());
        assertThat(loaded).isEqualTo(content);

        // Delete the file via storage key
        gateway.delete(stored.storageKey());

        // Assert file is gone and unrelated file is preserved
        assertThat(Files.exists(tempDir.resolve(stored.storageKey()).normalize())).isFalse();
        assertThat(Files.exists(unrelatedFile)).isTrue();
        assertThat(Files.readString(unrelatedFile, StandardCharsets.UTF_8)).isEqualTo("unrelated content");
    }

    @Test
    @DisplayName("CloudinaryStorageGateway: delete calls uploader.destroy with correct raw resource type and publicId")
    void cloudinaryStorageDeleteCallsDestroy() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);

        CloudinaryStorageGateway gateway = new CloudinaryStorageGateway(cloudinary);

        // Test with full URL
        String url = "https://res.cloudinary.com/test-cloud/raw/upload/v1612345678/vieva_docs/uuid-123_knowledge.txt";
        gateway.delete(url);

        verify(uploader).destroy(eq("vieva_docs/uuid-123_knowledge.txt"), eq(ObjectUtils.asMap("resource_type", "raw")));

        // Test with direct public ID
        String directKey = "vieva_docs/direct-key-456.txt";
        gateway.delete(directKey);

        verify(uploader).destroy(eq("vieva_docs/direct-key-456.txt"), eq(ObjectUtils.asMap("resource_type", "raw")));
    }
}
