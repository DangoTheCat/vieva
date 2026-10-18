package com.example.vieva.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryAvatarStorageGatewayTest {

    private CloudinaryAvatarStorageGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new CloudinaryAvatarStorageGateway(
                "cloudinary://test_key:test_secret@test_cloud",
                null, null, null
        );
    }

    @Test
    @DisplayName("extractPublicId: extracts publicId from standard Cloudinary URL with version and folder")
    void extractPublicId_StandardUrl_Success() {
        String url = "https://res.cloudinary.com/test_cloud/image/upload/v1729285000/vieva_avatars/avatar_123_456.jpg";
        String publicId = gateway.extractPublicId(url);
        assertThat(publicId).isEqualTo("vieva_avatars/avatar_123_456");
    }

    @Test
    @DisplayName("extractPublicId: extracts publicId without version")
    void extractPublicId_WithoutVersion_Success() {
        String url = "https://res.cloudinary.com/test_cloud/image/upload/vieva_avatars/avatar_123_456.png";
        String publicId = gateway.extractPublicId(url);
        assertThat(publicId).isEqualTo("vieva_avatars/avatar_123_456");
    }

    @Test
    @DisplayName("extractPublicId: extracts publicId with image transformation parameters")
    void extractPublicId_WithTransformations_Success() {
        String url = "https://res.cloudinary.com/test_cloud/image/upload/c_fill,w_300,h_300/v1729285000/vieva_avatars/avatar_user_789.webp";
        String publicId = gateway.extractPublicId(url);
        assertThat(publicId).isEqualTo("vieva_avatars/avatar_user_789");
    }

    @Test
    @DisplayName("extractPublicId: returns null for non-Cloudinary URL")
    void extractPublicId_NonCloudinaryUrl_ReturnsNull() {
        String url = "https://example.com/images/avatar.png";
        String publicId = gateway.extractPublicId(url);
        assertThat(publicId).isNull();
    }

    @Test
    @DisplayName("extractPublicId: returns null for null or empty string")
    void extractPublicId_NullOrEmpty_ReturnsNull() {
        assertThat(gateway.extractPublicId(null)).isNull();
        assertThat(gateway.extractPublicId("")).isNull();
    }
}
