package com.example.vieva.application.ports.output;

import java.util.UUID;

/**
 * Output port for uploading and managing user avatar images in external media storage (e.g., Cloudinary).
 */
public interface AvatarStoragePort {

    /**
     * Uploads user avatar image bytes and returns the secure public CDN URL.
     *
     * @param userId      the user UUID
     * @param imageBytes  raw image content
     * @param contentType MIME type of the image (e.g. image/jpeg, image/png)
     * @return secure HTTPS URL of the uploaded avatar
     */
    String uploadAvatar(UUID userId, byte[] imageBytes, String contentType);

    /**
     * Deletes user avatar image from external media storage (e.g. Cloudinary) by its CDN URL.
     *
     * @param avatarUrl the avatar secure CDN URL to delete
     */
    void deleteAvatar(String avatarUrl);
}

