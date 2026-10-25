package com.example.vieva.infrastructure.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "speech_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeechSettingsJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "locale", nullable = false, length = 50)
    private String locale;

    @Column(name = "voice", nullable = false, length = 100)
    private String voice;

    @Column(name = "stt_language", nullable = false, length = 50)
    private String sttLanguage;

    @Column(name = "tts_language", nullable = false, length = 50)
    private String ttsLanguage;

    @Column(name = "vietnamese_voice", nullable = false, length = 100)
    private String vietnameseVoice;

    @Column(name = "english_voice", length = 100)
    private String englishVoice;

    @Column(name = "speed", nullable = false, precision = 3, scale = 2)
    private BigDecimal speed;

    @Column(name = "pitch", nullable = false, precision = 3, scale = 2)
    private BigDecimal pitch;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
