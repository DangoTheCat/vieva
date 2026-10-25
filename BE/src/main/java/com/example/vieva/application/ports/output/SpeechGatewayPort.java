package com.example.vieva.application.ports.output;

import java.util.List;

public interface SpeechGatewayPort {
    List<String> getSupportedLocales();
    List<String> getSupportedVoices(String locale);
    boolean isVoiceSupported(String locale, String voice);
    boolean isSpeedSupported();
    boolean isPitchSupported();
    boolean isProviderAvailable();
}
