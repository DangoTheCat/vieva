package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SpeechSettings {
    public static final UUID DEFAULT_SETTINGS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UUID id;
    @Builder.Default
    private String locale = "vi-VN";
    @Builder.Default
    private String voice = "maichi";
    @Builder.Default
    private String sttLanguage = "vi-VN";
    @Builder.Default
    private String ttsLanguage = "vi-VN";
    @Builder.Default
    private String vietnameseVoice = "maichi";
    private String englishVoice;
    @Builder.Default
    private BigDecimal speed = BigDecimal.ONE;
    @Builder.Default
    private BigDecimal pitch = BigDecimal.ONE;
    private UUID updatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public void update(String locale, String voice, BigDecimal speed, BigDecimal pitch, UUID adminId) {
        if (locale != null && !locale.isBlank()) {
            this.locale = locale.trim();
            this.ttsLanguage = this.locale;
            this.sttLanguage = this.locale;
        }
        if (voice != null && !voice.isBlank()) {
            this.voice = voice.trim();
            this.vietnameseVoice = this.voice;
        }
        if (speed != null) {
            this.speed = speed;
        }
        if (pitch != null) {
            this.pitch = pitch;
        }
        this.updatedBy = adminId;
        this.updatedAt = Instant.now();
    }

    public void update(String locale, String voice, String sttLanguage, String ttsLanguage,
                       String vietnameseVoice, String englishVoice,
                       BigDecimal speed, BigDecimal pitch, UUID adminId) {
        if (sttLanguage != null && !sttLanguage.isBlank()) {
            this.sttLanguage = sttLanguage.trim();
        }
        if (ttsLanguage != null && !ttsLanguage.isBlank()) {
            this.ttsLanguage = ttsLanguage.trim();
            this.locale = this.ttsLanguage;
        }
        if (vietnameseVoice != null && !vietnameseVoice.isBlank()) {
            this.vietnameseVoice = vietnameseVoice.trim();
            if (this.ttsLanguage == null || "vi-VN".equalsIgnoreCase(this.ttsLanguage)) {
                this.voice = this.vietnameseVoice;
            }
        }
        this.englishVoice = (englishVoice != null && !englishVoice.isBlank()) ? englishVoice.trim() : null;
        if ("en-US".equalsIgnoreCase(this.ttsLanguage) && this.englishVoice != null) {
            this.voice = this.englishVoice;
        }

        if (locale != null && !locale.isBlank() && (ttsLanguage == null || ttsLanguage.isBlank())) {
            this.locale = locale.trim();
            this.ttsLanguage = this.locale;
            if (sttLanguage == null || sttLanguage.isBlank()) {
                this.sttLanguage = this.locale;
            }
        }
        if (voice != null && !voice.isBlank() && (vietnameseVoice == null || vietnameseVoice.isBlank())) {
            this.voice = voice.trim();
            this.vietnameseVoice = this.voice;
        }

        if (speed != null) {
            this.speed = speed;
        }
        if (pitch != null) {
            this.pitch = pitch;
        }
        this.updatedBy = adminId;
        this.updatedAt = Instant.now();
    }
}
