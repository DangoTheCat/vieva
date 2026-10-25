package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.SpeechSettings;
import org.springframework.stereotype.Component;

@Component
public class SpeechSettingsPersistenceMapper {

    public SpeechSettings toDomain(SpeechSettingsJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return SpeechSettings.builder()
                .id(entity.getId())
                .locale(entity.getLocale())
                .voice(entity.getVoice())
                .sttLanguage(entity.getSttLanguage() != null ? entity.getSttLanguage() : "vi-VN")
                .ttsLanguage(entity.getTtsLanguage() != null ? entity.getTtsLanguage() : entity.getLocale())
                .vietnameseVoice(entity.getVietnameseVoice() != null ? entity.getVietnameseVoice() : entity.getVoice())
                .englishVoice(entity.getEnglishVoice())
                .speed(entity.getSpeed())
                .pitch(entity.getPitch())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SpeechSettingsJpaEntity toEntity(SpeechSettings domain) {
        if (domain == null) {
            return null;
        }
        String stt = domain.getSttLanguage() != null && !domain.getSttLanguage().isBlank()
                ? domain.getSttLanguage()
                : (domain.getLocale() != null ? domain.getLocale() : "vi-VN");
        String tts = domain.getTtsLanguage() != null && !domain.getTtsLanguage().isBlank()
                ? domain.getTtsLanguage()
                : (domain.getLocale() != null ? domain.getLocale() : "vi-VN");
        String vnVoice = domain.getVietnameseVoice() != null && !domain.getVietnameseVoice().isBlank()
                ? domain.getVietnameseVoice()
                : (domain.getVoice() != null ? domain.getVoice() : "maichi");

        return SpeechSettingsJpaEntity.builder()
                .id(domain.getId())
                .locale(domain.getLocale() != null ? domain.getLocale() : tts)
                .voice(domain.getVoice() != null ? domain.getVoice() : vnVoice)
                .sttLanguage(stt)
                .ttsLanguage(tts)
                .vietnameseVoice(vnVoice)
                .englishVoice(domain.getEnglishVoice())
                .speed(domain.getSpeed())
                .pitch(domain.getPitch())
                .updatedBy(domain.getUpdatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
