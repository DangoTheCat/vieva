package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Phiên bản cấu hình dịch vụ thoại STT/TTS; kích hoạt nguyên tử sau khi thử nghiệm đạt chuẩn.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechConfigVersion {
    private UUID configVersionId;
    private String versionCode;
    private LanguageLocale languageLocale;
    private String sttProvider;
    private String ttsProvider;
    private String ttsVoiceCode;
    private BigDecimal ttsSpeechRate;
    private BigDecimal ttsPitch;
    private Boolean isActive;
    private SpeechTestStatus testStatus;
    private Instant testedAt;
    private String testSampleTranscript;
    private UUID createdBy;
    private Instant createdAt;
}
