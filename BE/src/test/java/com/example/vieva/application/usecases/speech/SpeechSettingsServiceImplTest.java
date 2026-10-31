package com.example.vieva.application.usecases.speech;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.speech.SpeechSettingsResult;
import com.example.vieva.application.ports.input.speech.UpdateSpeechSettingsRequest;
import com.example.vieva.application.ports.output.SpeechGatewayPort;
import com.example.vieva.application.ports.output.SpeechSettingsRepository;
import com.example.vieva.domain.entities.SpeechSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpeechSettingsServiceImplTest {

    private SpeechSettingsRepository settingsRepository;
    private SpeechGatewayPort speechGateway;
    private SpeechSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(SpeechSettingsRepository.class);
        speechGateway = mock(SpeechGatewayPort.class);
        // TTS/STT locale lists default to the engine locales unless a test says otherwise
        org.mockito.Mockito.doCallRealMethod().when(speechGateway).getSupportedTtsLocales();
        org.mockito.Mockito.doCallRealMethod().when(speechGateway).getSupportedSttLocales();
        service = new SpeechSettingsServiceImpl(settingsRepository, speechGateway);
    }

    @Test
    void acceptsEnglishSttAndBrowserTtsWhenGatewaySupportsThem() {
        when(settingsRepository.getSettings()).thenReturn(defaultSettings());
        when(settingsRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        org.mockito.Mockito.doReturn(List.of("vi-VN", "en-US")).when(speechGateway).getSupportedTtsLocales();
        org.mockito.Mockito.doReturn(List.of("vi-VN", "en-US")).when(speechGateway).getSupportedSttLocales();
        when(speechGateway.isVoiceSupported("vi-VN", "maichi")).thenReturn(true);

        var result = service.updateSettings(com.example.vieva.application.ports.input.speech.UpdateSpeechSettingsRequest.builder()
                .sttLanguage("en-US").ttsLanguage("en-US").build(), java.util.UUID.randomUUID());

        org.assertj.core.api.Assertions.assertThat(result.getSttLanguage()).isEqualTo("en-US");
        org.assertj.core.api.Assertions.assertThat(result.getTtsLanguage()).isEqualTo("en-US");
    }

    @Test
    void testSettingsSpeaksSampleForEngineLanguageAndFlagsBrowserVoiceOtherwise() {
        when(settingsRepository.getSettings()).thenReturn(defaultSettings());
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        org.mockito.Mockito.doReturn(List.of("vi-VN", "en-US")).when(speechGateway).getSupportedTtsLocales();
        org.mockito.Mockito.doReturn(List.of("vi-VN", "en-US")).when(speechGateway).getSupportedSttLocales();
        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(speechGateway.isSttAvailable()).thenReturn(true);
        when(speechGateway.synthesizeSample(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(new byte[]{1, 2, 3}));

        var vi = service.testSettings("vi-VN", "vi-VN", null, null);
        org.assertj.core.api.Assertions.assertThat(vi.audioBase64()).isEqualTo("AQID");
        org.assertj.core.api.Assertions.assertThat(vi.browserVoice()).isFalse();

        var en = service.testSettings("en-US", "en-US", null, null);
        org.assertj.core.api.Assertions.assertThat(en.audioBase64()).isNull();
        org.assertj.core.api.Assertions.assertThat(en.browserVoice()).isTrue();
        org.assertj.core.api.Assertions.assertThat(en.sttAvailable()).isTrue();
    }

    private SpeechSettings defaultSettings() {
        return SpeechSettings.builder()
                .id(SpeechSettings.DEFAULT_SETTINGS_ID)
                .locale("vi-VN")
                .voice("maichi")
                .sttLanguage("vi-VN")
                .ttsLanguage("vi-VN")
                .vietnameseVoice("maichi")
                .englishVoice(null)
                .speed(BigDecimal.ONE)
                .pitch(BigDecimal.ONE)
                .updatedBy(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Throws AI_SERVICE_UNAVAILABLE when provider is not reachable")
    void throwsWhenProviderUnavailable() {
        UUID adminId = UUID.randomUUID();
        when(speechGateway.isProviderAvailable()).thenReturn(false);

        UpdateSpeechSettingsRequest req = new UpdateSpeechSettingsRequest();
        req.setLocale("vi-VN");
        req.setVoice("maichi");

        assertThatThrownBy(() -> service.updateSettings(req, adminId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);

        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws INVALID_SPEECH_LOCALE when locale is unsupported")
    void throwsWhenLocaleUnsupported() {
        UUID adminId = UUID.randomUUID();
        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(defaultSettings());
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));

        UpdateSpeechSettingsRequest req = new UpdateSpeechSettingsRequest();
        req.setLocale("fr-FR");

        assertThatThrownBy(() -> service.updateSettings(req, adminId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SPEECH_LOCALE);

        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws INVALID_SPEECH_VOICE when voice is unsupported")
    void throwsWhenVoiceUnsupported() {
        UUID adminId = UUID.randomUUID();
        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(defaultSettings());
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        when(speechGateway.isVoiceSupported("vi-VN", "unsupported_voice")).thenReturn(false);

        UpdateSpeechSettingsRequest req = new UpdateSpeechSettingsRequest();
        req.setLocale("vi-VN");
        req.setVoice("unsupported_voice");

        assertThatThrownBy(() -> service.updateSettings(req, adminId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SPEECH_VOICE);

        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws INVALID_REQUEST when modifying speed while speed is unsupported")
    void throwsWhenSpeedModifiedAndUnsupported() {
        UUID adminId = UUID.randomUUID();
        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(defaultSettings());
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        when(speechGateway.isVoiceSupported("vi-VN", "maichi")).thenReturn(true);
        when(speechGateway.isSpeedSupported()).thenReturn(false);

        UpdateSpeechSettingsRequest req = new UpdateSpeechSettingsRequest();
        req.setLocale("vi-VN");
        req.setVoice("maichi");
        req.setSpeed(BigDecimal.valueOf(1.5));

        assertThatThrownBy(() -> service.updateSettings(req, adminId))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updates settings successfully when inputs are valid and supported")
    void updatesSettingsSuccessfully() {
        UUID adminId = UUID.randomUUID();
        SpeechSettings current = defaultSettings();

        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(current);
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        when(speechGateway.isVoiceSupported("vi-VN", "maichi")).thenReturn(true);
        when(speechGateway.isSpeedSupported()).thenReturn(false);
        when(speechGateway.isPitchSupported()).thenReturn(false);

        when(settingsRepository.save(any(SpeechSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSpeechSettingsRequest req = new UpdateSpeechSettingsRequest();
        req.setLocale("vi-VN");
        req.setVoice("maichi");
        req.setSpeed(BigDecimal.ONE);
        req.setPitch(BigDecimal.ONE);

        SpeechSettingsResult result = service.updateSettings(req, adminId);

        assertThat(result).isNotNull();
        assertThat(result.getLocale()).isEqualTo("vi-VN");
        assertThat(result.getVoice()).isEqualTo("maichi");
        assertThat(result.getSttLanguage()).isEqualTo("vi-VN");
        assertThat(result.getTtsLanguage()).isEqualTo("vi-VN");
        assertThat(result.getVietnameseVoice()).isEqualTo("maichi");
        assertThat(result.getEnglishVoice()).isNull();
        assertThat(result.getSpeed()).isEqualTo(BigDecimal.ONE);
        assertThat(result.getPitch()).isEqualTo(BigDecimal.ONE);
        assertThat(result.getUpdatedBy()).isEqualTo(adminId);

        verify(settingsRepository).save(current);
    }

    @Test
    @DisplayName("Updates new language and voice settings successfully")
    void updatesNewLanguageSettingsSuccessfully() {
        UUID adminId = UUID.randomUUID();
        SpeechSettings current = defaultSettings();

        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(current);
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        when(speechGateway.isVoiceSupported("vi-VN", "maichi")).thenReturn(true);
        when(speechGateway.isSpeedSupported()).thenReturn(false);
        when(speechGateway.isPitchSupported()).thenReturn(false);
        when(settingsRepository.save(any(SpeechSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSpeechSettingsRequest req = UpdateSpeechSettingsRequest.builder()
                .sttLanguage("vi-VN")
                .ttsLanguage("vi-VN")
                .vietnameseVoice("maichi")
                .englishVoice(null)
                .speed(BigDecimal.ONE)
                .pitch(BigDecimal.ONE)
                .build();

        SpeechSettingsResult result = service.updateSettings(req, adminId);

        assertThat(result).isNotNull();
        assertThat(result.getSttLanguage()).isEqualTo("vi-VN");
        assertThat(result.getTtsLanguage()).isEqualTo("vi-VN");
        assertThat(result.getVietnameseVoice()).isEqualTo("maichi");
        assertThat(result.getEnglishVoice()).isNull();
        assertThat(result.getLocale()).isEqualTo("vi-VN");
        assertThat(result.getVoice()).isEqualTo("maichi");
        assertThat(result.getUpdatedBy()).isEqualTo(adminId);
    }

    @Test
    @DisplayName("Throws INVALID_SPEECH_VOICE when English voice is unsupported by gateway")
    void rejectsUnsupportedEnglishVoiceHonesty() {
        UUID adminId = UUID.randomUUID();
        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(defaultSettings());
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        when(speechGateway.isVoiceSupported("vi-VN", "maichi")).thenReturn(true);
        when(speechGateway.isVoiceSupported("en-US", "unsupported-en-voice")).thenReturn(false);

        UpdateSpeechSettingsRequest req = UpdateSpeechSettingsRequest.builder()
                .sttLanguage("vi-VN")
                .ttsLanguage("vi-VN")
                .vietnameseVoice("maichi")
                .englishVoice("unsupported-en-voice")
                .build();

        assertThatThrownBy(() -> service.updateSettings(req, adminId))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("English voice is not supported")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SPEECH_VOICE);

        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Preserves legacy request with only locale and voice, keeping new fields in sync")
    void preservesLegacyRequestCompatibly() {
        UUID adminId = UUID.randomUUID();
        SpeechSettings current = defaultSettings();

        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(current);
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        when(speechGateway.isVoiceSupported("vi-VN", "maichi")).thenReturn(true);
        when(speechGateway.isSpeedSupported()).thenReturn(false);
        when(speechGateway.isPitchSupported()).thenReturn(false);
        when(settingsRepository.save(any(SpeechSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSpeechSettingsRequest req = new UpdateSpeechSettingsRequest();
        req.setLocale("vi-VN");
        req.setVoice("maichi");

        SpeechSettingsResult result = service.updateSettings(req, adminId);

        assertThat(result).isNotNull();
        assertThat(result.getLocale()).isEqualTo("vi-VN");
        assertThat(result.getVoice()).isEqualTo("maichi");
        assertThat(result.getSttLanguage()).isEqualTo("vi-VN");
        assertThat(result.getTtsLanguage()).isEqualTo("vi-VN");
        assertThat(result.getVietnameseVoice()).isEqualTo("maichi");
        assertThat(result.getEnglishVoice()).isNull();
    }

    @Test
    @DisplayName("PATCH with omitted englishVoice preserves existing englishVoice")
    void omittedEnglishVoicePreservesExisting() {
        UUID adminId = UUID.randomUUID();
        SpeechSettings currentWithEnglish = SpeechSettings.builder()
                .id(SpeechSettings.DEFAULT_SETTINGS_ID)
                .locale("vi-VN")
                .voice("maichi")
                .sttLanguage("vi-VN")
                .ttsLanguage("vi-VN")
                .vietnameseVoice("maichi")
                .englishVoice("en-test-voice")
                .speed(BigDecimal.ONE)
                .pitch(BigDecimal.ONE)
                .updatedBy(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(currentWithEnglish);
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        when(speechGateway.isVoiceSupported("vi-VN", "maichi")).thenReturn(true);
        when(settingsRepository.save(any(SpeechSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        // Request with englishVoice == null (omitted in PATCH)
        UpdateSpeechSettingsRequest req = UpdateSpeechSettingsRequest.builder()
                .vietnameseVoice("maichi")
                .englishVoice(null)
                .build();

        SpeechSettingsResult result = service.updateSettings(req, adminId);

        assertThat(result).isNotNull();
        assertThat(result.getEnglishVoice()).isEqualTo("en-test-voice");
    }

    @Test
    @DisplayName("PATCH with blank englishVoice explicitly clears englishVoice to null")
    void blankEnglishVoiceClearsToNull() {
        UUID adminId = UUID.randomUUID();
        SpeechSettings currentWithEnglish = SpeechSettings.builder()
                .id(SpeechSettings.DEFAULT_SETTINGS_ID)
                .locale("vi-VN")
                .voice("maichi")
                .sttLanguage("vi-VN")
                .ttsLanguage("vi-VN")
                .vietnameseVoice("maichi")
                .englishVoice("en-test-voice")
                .speed(BigDecimal.ONE)
                .pitch(BigDecimal.ONE)
                .updatedBy(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(speechGateway.isProviderAvailable()).thenReturn(true);
        when(settingsRepository.getSettings()).thenReturn(currentWithEnglish);
        when(speechGateway.getSupportedLocales()).thenReturn(List.of("vi-VN"));
        when(speechGateway.isVoiceSupported("vi-VN", "maichi")).thenReturn(true);
        when(settingsRepository.save(any(SpeechSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        // Request with englishVoice == "   " (explicit clear)
        UpdateSpeechSettingsRequest req = UpdateSpeechSettingsRequest.builder()
                .vietnameseVoice("maichi")
                .englishVoice("   ")
                .build();

        SpeechSettingsResult result = service.updateSettings(req, adminId);

        assertThat(result).isNotNull();
        assertThat(result.getEnglishVoice()).isNull();
    }

    @Test
    @DisplayName("getSettings returns full result with defaults")
    void getSettingsReturnsFullResultWithDefaults() {
        when(settingsRepository.getSettings()).thenReturn(defaultSettings());

        SpeechSettingsResult result = service.getSettings();

        assertThat(result).isNotNull();
        assertThat(result.getLocale()).isEqualTo("vi-VN");
        assertThat(result.getVoice()).isEqualTo("maichi");
        assertThat(result.getSttLanguage()).isEqualTo("vi-VN");
        assertThat(result.getTtsLanguage()).isEqualTo("vi-VN");
        assertThat(result.getVietnameseVoice()).isEqualTo("maichi");
        assertThat(result.getEnglishVoice()).isNull();
    }
}
