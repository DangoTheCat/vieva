package com.example.vieva.application.ports.input.speech;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class SpeechSettingsResult {
    UUID id;
    String locale;
    String voice;
    String sttLanguage;
    String ttsLanguage;
    String vietnameseVoice;
    String englishVoice;
    BigDecimal speed;
    BigDecimal pitch;
    UUID updatedBy;
    Instant updatedAt;
}
