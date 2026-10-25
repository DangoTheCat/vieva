package com.example.vieva.application.ports.input.speech;

import java.util.UUID;

public interface SpeechSettingsService {
    SpeechSettingsResult getSettings();
    SpeechSettingsResult updateSettings(UpdateSpeechSettingsRequest request, UUID adminId);
}
