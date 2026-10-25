package com.example.vieva.application.ports.input.speech;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSpeechSettingsRequest {
    private String locale;
    private String voice;
    private String sttLanguage;
    private String ttsLanguage;
    private String vietnameseVoice;
    private String englishVoice;
    private BigDecimal speed;
    private BigDecimal pitch;
}
