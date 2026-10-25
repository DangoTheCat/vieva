package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.SpeechSettings;

import java.util.Optional;

public interface SpeechSettingsRepository {
    SpeechSettings getSettings();
    SpeechSettings save(SpeechSettings settings);
}
