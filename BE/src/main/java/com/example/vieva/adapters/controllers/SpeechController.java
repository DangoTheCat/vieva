package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.speech.SpeechTestResult;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.speech.SpeechSettingsResult;
import com.example.vieva.application.ports.input.speech.SpeechSettingsService;
import com.example.vieva.application.ports.input.speech.UpdateSpeechSettingsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SpeechController {

    private final SpeechSettingsService speechSettingsService;

    public record UpdateSpeechApiRequest(
            String locale,
            String voice,
            String sttLanguage,
            String ttsLanguage,
            String vietnameseVoice,
            String englishVoice,
            BigDecimal speed,
            BigDecimal pitch
    ) {}

    @GetMapping("/admin/speech")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpeechSettingsResult> getAdminSpeechSettings() {
        return ResponseEntity.ok(speechSettingsService.getSettings());
    }

    @PatchMapping("/admin/speech")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpeechSettingsResult> updateSpeechSettings(
            @AuthenticationPrincipal ActorView currentAdmin,
            @Valid @RequestBody UpdateSpeechApiRequest request
    ) {
        UUID adminId = CurrentUser.id(currentAdmin);
        UpdateSpeechSettingsRequest useCaseRequest = UpdateSpeechSettingsRequest.builder()
                .locale(request.locale())
                .voice(request.voice())
                .sttLanguage(request.sttLanguage())
                .ttsLanguage(request.ttsLanguage())
                .vietnameseVoice(request.vietnameseVoice())
                .englishVoice(request.englishVoice())
                .speed(request.speed())
                .pitch(request.pitch())
                .build();
        return ResponseEntity.ok(speechSettingsService.updateSettings(useCaseRequest, adminId));
    }

    public record TestSpeechApiRequest(
            String ttsLanguage,
            String sttLanguage,
            String voice,
            String sampleText
    ) {}

    /** Tries a configuration (speaks a sample, checks STT) without saving it. */
    @PostMapping("/admin/speech/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpeechTestResult> testSpeechSettings(@RequestBody(required = false) TestSpeechApiRequest request) {
        TestSpeechApiRequest r = request != null ? request : new TestSpeechApiRequest(null, null, null, null);
        return ResponseEntity.ok(speechSettingsService.testSettings(r.ttsLanguage(), r.sttLanguage(), r.voice(), r.sampleText()));
    }

    @GetMapping("/student/speech")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpeechSettingsResult> getStudentSpeechSettings() {
        return ResponseEntity.ok(speechSettingsService.getSettings());
    }
}
