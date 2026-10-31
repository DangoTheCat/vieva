package com.example.vieva.application.ports.input.speech;

import java.util.UUID;

public interface SpeechSettingsService {
    SpeechSettingsResult getSettings();
    SpeechSettingsResult updateSettings(UpdateSpeechSettingsRequest request, UUID adminId);

    /** Tries a configuration without saving it: speaks a sample and checks the STT service. */
    SpeechTestResult testSettings(String ttsLanguage, String sttLanguage, String voice, String sampleText);
}
