package com.example.vieva.application.usecases.speech;

import com.example.vieva.application.ports.input.speech.SpeechTestResult;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.speech.SpeechSettingsResult;
import com.example.vieva.application.ports.input.speech.SpeechSettingsService;
import com.example.vieva.application.ports.input.speech.UpdateSpeechSettingsRequest;
import com.example.vieva.application.ports.output.SpeechGatewayPort;
import com.example.vieva.application.ports.output.SpeechSettingsRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.SpeechSettings;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
@UseCaseTransaction
public class SpeechSettingsServiceImpl implements SpeechSettingsService {

    private final SpeechSettingsRepository settingsRepository;
    private final SpeechGatewayPort speechGateway;

    @Override
    @UseCaseTransaction(readOnly = true)
    public SpeechSettingsResult getSettings() {
        SpeechSettings settings = settingsRepository.getSettings();
        return toResult(settings);
    }

    @Override
    public SpeechSettingsResult updateSettings(UpdateSpeechSettingsRequest request, UUID adminId) {
        if (!speechGateway.isProviderAvailable()) {
            throw new ApplicationException(ErrorCode.AI_SERVICE_UNAVAILABLE, "TTS provider is currently unavailable");
        }

        SpeechSettings settings = settingsRepository.getSettings();

        // 1. Resolve TTS language / locale
        String targetTtsLanguage = request.getTtsLanguage() != null && !request.getTtsLanguage().isBlank()
                ? request.getTtsLanguage().trim()
                : (request.getLocale() != null && !request.getLocale().isBlank()
                        ? request.getLocale().trim()
                        : (settings.getTtsLanguage() != null ? settings.getTtsLanguage() : settings.getLocale()));
        if (targetTtsLanguage == null || targetTtsLanguage.isBlank()) {
            targetTtsLanguage = "vi-VN";
        }
        if (!speechGateway.getSupportedTtsLocales().contains(targetTtsLanguage)) {
            throw new ApplicationException(ErrorCode.INVALID_SPEECH_LOCALE);
        }

        // 2. Resolve STT language
        String targetSttLanguage = request.getSttLanguage() != null && !request.getSttLanguage().isBlank()
                ? request.getSttLanguage().trim()
                : (request.getLocale() != null && !request.getLocale().isBlank()
                        ? request.getLocale().trim()
                        : (settings.getSttLanguage() != null ? settings.getSttLanguage() : "vi-VN"));
        if (targetSttLanguage == null || targetSttLanguage.isBlank()) {
            targetSttLanguage = "vi-VN";
        }
        if (!speechGateway.getSupportedSttLocales().contains(targetSttLanguage)) {
            throw new ApplicationException(ErrorCode.INVALID_SPEECH_LOCALE);
        }

        // 3. Resolve Vietnamese voice
        String targetVnVoice = request.getVietnameseVoice() != null && !request.getVietnameseVoice().isBlank()
                ? request.getVietnameseVoice().trim()
                : (request.getVoice() != null && !request.getVoice().isBlank()
                        ? request.getVoice().trim()
                        : (settings.getVietnameseVoice() != null ? settings.getVietnameseVoice() : settings.getVoice()));
        if (targetVnVoice == null || targetVnVoice.isBlank()) {
            targetVnVoice = "maichi";
        }
        if (!speechGateway.isVoiceSupported("vi-VN", targetVnVoice)) {
            throw new ApplicationException(ErrorCode.INVALID_SPEECH_VOICE);
        }

        // 4. Resolve English voice (optional):
        // - null (omitted in PATCH): preserve existing settings.getEnglishVoice()
        // - blank (""): explicit clear to null
        // - non-blank: validate with gateway capability and update
        String targetEnVoice;
        if (request.getEnglishVoice() == null) {
            targetEnVoice = settings.getEnglishVoice();
        } else if (request.getEnglishVoice().isBlank()) {
            targetEnVoice = null;
        } else {
            String candidateEnVoice = request.getEnglishVoice().trim();
            if (!speechGateway.isVoiceSupported("en-US", candidateEnVoice)) {
                throw new ApplicationException(ErrorCode.INVALID_SPEECH_VOICE, "English voice is not supported");
            }
            targetEnVoice = candidateEnVoice;
        }

        BigDecimal speed = request.getSpeed() != null ? request.getSpeed() : settings.getSpeed();
        BigDecimal pitch = request.getPitch() != null ? request.getPitch() : settings.getPitch();

        if (speed.compareTo(BigDecimal.valueOf(0.25)) < 0 || speed.compareTo(BigDecimal.valueOf(4.0)) > 0) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Speed must be between 0.25 and 4.0");
        }
        if (pitch.compareTo(BigDecimal.valueOf(0.25)) < 0 || pitch.compareTo(BigDecimal.valueOf(4.0)) > 0) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Pitch must be between 0.25 and 4.0");
        }

        if (speed.compareTo(BigDecimal.ONE) != 0 && !speechGateway.isSpeedSupported()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Speech speed adjustment is not supported by TTS provider");
        }
        if (pitch.compareTo(BigDecimal.ONE) != 0 && !speechGateway.isPitchSupported()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Speech pitch adjustment is not supported by TTS provider");
        }

        String effectiveVoice = "en-US".equalsIgnoreCase(targetTtsLanguage) && targetEnVoice != null
                ? targetEnVoice
                : targetVnVoice;

        settings.update(
                targetTtsLanguage,
                effectiveVoice,
                targetSttLanguage,
                targetTtsLanguage,
                targetVnVoice,
                targetEnVoice,
                speed,
                pitch,
                adminId
        );
        SpeechSettings saved = settingsRepository.save(settings);
        return toResult(saved);
    }

    @Override
    public SpeechTestResult testSettings(String ttsLanguage, String sttLanguage, String voice, String sampleText) {
        SpeechSettings current = settingsRepository.getSettings();
        String tts = ttsLanguage != null && !ttsLanguage.isBlank() ? ttsLanguage.trim()
                : (current.getTtsLanguage() != null ? current.getTtsLanguage() : "vi-VN");
        String stt = sttLanguage != null && !sttLanguage.isBlank() ? sttLanguage.trim()
                : (current.getSttLanguage() != null ? current.getSttLanguage() : "vi-VN");
        if (!speechGateway.getSupportedTtsLocales().contains(tts) || !speechGateway.getSupportedSttLocales().contains(stt)) {
            throw new ApplicationException(ErrorCode.INVALID_SPEECH_LOCALE);
        }
        boolean english = tts.toLowerCase().startsWith("en");
        String chosenVoice = voice != null && !voice.isBlank() ? voice.trim()
                : (english ? current.getEnglishVoice() : current.getVietnameseVoice());
        String text = sampleText != null && !sampleText.isBlank() ? sampleText.trim()
                : (english ? "Hello, this is the voice that will read your exam questions."
                           : "Xin chào, đây là giọng đọc câu hỏi trong phòng thi vấn đáp.");

        boolean ttsAvailable = speechGateway.isProviderAvailable();
        boolean sttAvailable = speechGateway.isSttAvailable();
        boolean engineSpeaksLanguage = speechGateway.getSupportedLocales().contains(tts);

        String audioBase64 = null;
        if (ttsAvailable && engineSpeaksLanguage) {
            audioBase64 = speechGateway.synthesizeSample(text, chosenVoice)
                    .map(bytes -> java.util.Base64.getEncoder().encodeToString(bytes))
                    .orElse(null);
        }
        boolean browserVoice = !engineSpeaksLanguage;

        String message;
        if (!sttAvailable) {
            message = "Dịch vụ nhận dạng giọng nói (STT) chưa phản hồi; sinh viên sẽ không thấy chữ khi trả lời.";
        } else if (browserVoice) {
            message = "Ngôn ngữ này được đọc bằng giọng của trình duyệt sinh viên (máy TTS chỉ hỗ trợ tiếng Việt).";
        } else if (audioBase64 == null) {
            message = "Máy TTS chưa tạo được câu mẫu; phòng thi sẽ dùng giọng của trình duyệt.";
        } else {
            message = "Thử nghiệm thành công. Có thể lưu cấu hình.";
        }
        return new SpeechTestResult(ttsAvailable, sttAvailable, tts, stt, chosenVoice, text, audioBase64, browserVoice, message);
    }

    private SpeechSettingsResult toResult(SpeechSettings s) {
        String effectiveLocale = s.getLocale() != null ? s.getLocale() : "vi-VN";
        String effectiveVoice = s.getVoice() != null ? s.getVoice() : "maichi";
        return SpeechSettingsResult.builder()
                .id(s.getId())
                .locale(effectiveLocale)
                .voice(effectiveVoice)
                .sttLanguage(s.getSttLanguage() != null ? s.getSttLanguage() : effectiveLocale)
                .ttsLanguage(s.getTtsLanguage() != null ? s.getTtsLanguage() : effectiveLocale)
                .vietnameseVoice(s.getVietnameseVoice() != null ? s.getVietnameseVoice() : effectiveVoice)
                .englishVoice(s.getEnglishVoice())
                .speed(s.getSpeed())
                .pitch(s.getPitch())
                .updatedBy(s.getUpdatedBy())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
