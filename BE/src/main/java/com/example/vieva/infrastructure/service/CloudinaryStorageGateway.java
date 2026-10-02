package com.example.vieva.infrastructure.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.vieva.application.ports.output.FileStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CloudinaryStorageGateway implements FileStoragePort {

    private final Cloudinary cloudinary;

    public CloudinaryStorageGateway(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        if (StringUtils.hasText(cloudName) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret)) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret));
        } else {
            this.cloudinary = null;
        }
    }

    @Override
    public String uploadFile(String filename, byte[] content, String contentType) {
        if (cloudinary != null) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                        "resource_type", "auto",
                        "public_id", "vieva_docs/" + UUID.randomUUID() + "_" + filename));
                return (String) uploadResult.get("secure_url");
            } catch (Exception e) {
                log.error("Cloudinary upload failed, falling back to storage resource identifier", e);
            }
        }
        return "https://storage.vieva.edu.vn/documents/" + UUID.randomUUID() + "/" + filename;
    }
}
