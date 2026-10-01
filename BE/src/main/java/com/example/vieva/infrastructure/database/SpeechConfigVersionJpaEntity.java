package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.LanguageLocale;
import com.example.vieva.domain.entities.SpeechTestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Phiên bản cấu hình dịch vụ thoại STT/TTS; kích hoạt nguyên tử sau khi thử nghiệm đạt chuẩn.
 */
@Entity
// "At most one active config per language" is enforced by a PARTIAL unique index managed
// by Flyway: CREATE UNIQUE INDEX uq_speech_config_active ON (language_locale) WHERE is_active.
@Table(name = "speech_config_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeechConfigVersionJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "config_version_id", updatable = false, nullable = false)
    private UUID configVersionId;

    @Column(name = "version_code", nullable = false, unique = true, length = 50)
    private String versionCode;

    @Convert(converter = LanguageLocaleConverter.class)
    @Column(name = "language_locale", nullable = false, length = 20)
    private LanguageLocale languageLocale;

    @Column(name = "stt_provider", nullable = false, length = 50)
    private String sttProvider;

    @Column(name = "tts_provider", nullable = false, length = 50)
    private String ttsProvider;

    @Column(name = "tts_voice_code", nullable = false, length = 50)
    private String ttsVoiceCode;

    @Builder.Default
    @Column(name = "tts_speech_rate", nullable = false, precision = 3, scale = 2)
    private BigDecimal ttsSpeechRate = new BigDecimal("1.00");

    @Builder.Default
    @Column(name = "tts_pitch", nullable = false, precision = 3, scale = 2)
    private BigDecimal ttsPitch = new BigDecimal("1.00");

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "test_status", nullable = false, length = 20)
    private SpeechTestStatus testStatus = SpeechTestStatus.NOT_TESTED;

    @Column(name = "tested_at")
    private Instant testedAt;

    @Column(name = "test_sample_transcript", columnDefinition = "TEXT")
    private String testSampleTranscript;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Optimistic locking — prevents lost updates when a config is tested and
     * activated concurrently (two admins must not both flip is_active).
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return configVersionId;
    }

    @Override
    public boolean isNew() {
        return isNew || createdAt == null;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
