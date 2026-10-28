package com.example.vieva.infrastructure.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.vieva.application.ports.output.AvatarStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Cloudinary-backed implementation of {@link AvatarStoragePort}.
 * Uploads user avatars to Cloudinary CDN and returns their HTTPS secure URL.
 */
@Slf4j
@Service
public class CloudinaryAvatarStorageGateway implements AvatarStoragePort {

    private final Cloudinary cloudinary;

    public CloudinaryAvatarStorageGateway(
            @Value("${cloudinary.url:}") String cloudinaryUrl,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {

        if (StringUtils.hasText(cloudinaryUrl)) {
            log.info("Initializing Cloudinary avatar storage with CLOUDINARY_URL");
            this.cloudinary = new Cloudinary(cloudinaryUrl);
        } else if (StringUtils.hasText(cloudName) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret)) {
            log.info("Initializing Cloudinary avatar storage with cloud-name: {}", cloudName);
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true));
        } else {
            log.warn("Cloudinary credentials not provided in environment. Initializing empty client.");
            this.cloudinary = new Cloudinary(ObjectUtils.emptyMap());
        }
    }

    private static final String FOLDER_NAME = "vieva_avatars";
    private static final java.util.regex.Pattern PUBLIC_ID_PATTERN =
            java.util.regex.Pattern.compile("(?:(?:s--[^/]+--/)|(?:[a-z0-9_]+,[^/]+/))*(?:v\\d+/)?([^?#]+)");

    @Override
    public String uploadAvatar(UUID userId, byte[] imageBytes, String contentType) {
        try {
            Map<?, ?> uploadParams = ObjectUtils.asMap(
                    "folder", FOLDER_NAME,
                    "public_id", "avatar_" + userId + "_" + System.currentTimeMillis(),
                    "overwrite", true,
                    "resource_type", "image"
            );
            Map<?, ?> result = cloudinary.uploader().upload(imageBytes, uploadParams);
            String secureUrl = (String) result.get("secure_url");
            log.info("Successfully uploaded avatar to Cloudinary for user {}: {}", userId, secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Cloudinary avatar upload failed for user {}: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("Tải ảnh đại diện lên Cloudinary thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteAvatar(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return;
        }
        String publicId = extractPublicId(avatarUrl);
        if (!StringUtils.hasText(publicId)) {
            log.warn("Could not extract Cloudinary public_id from avatar URL: {}", avatarUrl);
            return;
        }
        try {
            log.info("Deleting old avatar from Cloudinary with public_id: {}", publicId);
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary destroy result for {}: {}", publicId, result);
        } catch (Exception e) {
            log.error("Failed to delete avatar from Cloudinary (public_id: {}): {}", publicId, e.getMessage(), e);
        }
    }

    /**
     * Extracts the Cloudinary public_id (including folder path) from a secure CDN URL.
     * e.g. "https://res.cloudinary.com/demo_cloud/image/upload/v1729285000/vieva_avatars/avatar_123_456.jpg"
     * -> "vieva_avatars/avatar_123_456"
     */
    public String extractPublicId(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.contains("/upload/")) {
            return null;
        }
        try {
            String afterUpload = avatarUrl.substring(avatarUrl.indexOf("/upload/") + 8);
            int queryIdx = afterUpload.indexOf('?');
            if (queryIdx >= 0) {
                afterUpload = afterUpload.substring(0, queryIdx);
            }
            java.util.regex.Matcher matcher = PUBLIC_ID_PATTERN.matcher(afterUpload);
            if (matcher.find()) {
                String fullPath = matcher.group(1);
                int dotIdx = fullPath.lastIndexOf('.');
                if (dotIdx > 0) {
                    fullPath = fullPath.substring(0, dotIdx);
                }
                return java.net.URLDecoder.decode(fullPath, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Failed to parse public_id from avatar URL {}: {}", avatarUrl, e.getMessage());
        }
        return null;
    }
}
