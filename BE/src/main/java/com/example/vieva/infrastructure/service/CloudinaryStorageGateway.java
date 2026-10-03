package com.example.vieva.infrastructure.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.vieva.application.ports.output.FileStoragePort;
import com.example.vieva.application.ports.output.StoredFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Cloudinary storage ({@code vieva.storage.provider=cloudinary}). Documents are uploaded as "raw"
 * resources so {@link #load} returns the exact bytes for re-indexing.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vieva.storage.provider", havingValue = "cloudinary")
public class CloudinaryStorageGateway implements FileStoragePort {

    private final Cloudinary cloudinary;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public CloudinaryStorageGateway(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        if (!StringUtils.hasText(cloudName) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) {
            throw new IllegalStateException("Cloudinary storage selected but CLOUDINARY_* variables are missing");
        }
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    @Override
    public StoredFile store(String filename, byte[] content, String contentType) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                    "resource_type", "raw",
                    "public_id", "vieva_docs/" + UUID.randomUUID() + "_" + filename));
            String url = (String) result.get("secure_url");
            return new StoredFile(url, url);
        } catch (IOException e) {
            throw new IllegalStateException("Cloudinary upload failed", e);
        }
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(storageKey))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Cloudinary download failed with HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Cloudinary download failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cloudinary download interrupted", e);
        }
    }
}
