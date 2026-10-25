package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.SpeechSettingsRepository;
import com.example.vieva.domain.entities.SpeechSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class SpeechSettingsRepositoryImpl implements SpeechSettingsRepository {

    private final SpeechSettingsJpaRepository jpaRepository;
    private final SpeechSettingsPersistenceMapper mapper;

    @Override
    public SpeechSettings getSettings() {
        return jpaRepository.findById(SpeechSettings.DEFAULT_SETTINGS_ID)
                .map(mapper::toDomain)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    SpeechSettings fallback = SpeechSettings.builder()
                            .id(SpeechSettings.DEFAULT_SETTINGS_ID)
                            .locale("vi-VN")
                            .voice("maichi")
                            .sttLanguage("vi-VN")
                            .ttsLanguage("vi-VN")
                            .vietnameseVoice("maichi")
                            .englishVoice(null)
                            .speed(BigDecimal.valueOf(1.0))
                            .pitch(BigDecimal.valueOf(1.0))
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return save(fallback);
                });
    }

    @Override
    public SpeechSettings save(SpeechSettings settings) {
        SpeechSettingsJpaEntity entity = mapper.toEntity(settings);
        SpeechSettingsJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
