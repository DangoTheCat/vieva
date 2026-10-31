package com.example.vieva.application.ports.output;

import java.util.List;
import java.util.Optional;

public interface SpeechGatewayPort {
    /** Locales the TTS engine itself can speak. */
    List<String> getSupportedLocales();

    /** Locales the exam room may read questions in (engine locales plus browser-voice locales). */
    default List<String> getSupportedTtsLocales() {
        return getSupportedLocales();
    }

    /** Locales the STT service can transcribe. */
    default List<String> getSupportedSttLocales() {
        return getSupportedLocales();
    }

    /** Synthesizes a short sample with the TTS engine; empty when the engine cannot speak it. */
    default Optional<byte[]> synthesizeSample(String text, String voice) {
        return Optional.empty();
    }

    /** True when the STT service answers its health check. */
    default boolean isSttAvailable() {
        return false;
    }

    List<String> getSupportedVoices(String locale);
    boolean isVoiceSupported(String locale, String voice);
    boolean isSpeedSupported();
    boolean isPitchSupported();
    boolean isProviderAvailable();
}
