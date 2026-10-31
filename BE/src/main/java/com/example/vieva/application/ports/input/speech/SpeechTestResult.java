package com.example.vieva.application.ports.input.speech;

/**
 * Result of trying a voice configuration before saving it (WF07: configure -> test -> activate).
 *
 * @param audioBase64   sample spoken by the TTS engine (WAV), null when the browser voice is used
 * @param browserVoice  true when this language is read by the student's browser (no engine voice)
 */
public record SpeechTestResult(
        boolean ttsAvailable,
        boolean sttAvailable,
        String ttsLanguage,
        String sttLanguage,
        String voice,
        String sampleText,
        String audioBase64,
        boolean browserVoice,
        String message
) {
}
