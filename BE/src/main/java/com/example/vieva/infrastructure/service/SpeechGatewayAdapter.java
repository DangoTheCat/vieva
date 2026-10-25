package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.SpeechGatewayPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class SpeechGatewayAdapter implements SpeechGatewayPort {

    private final String baseUrl;
    private final List<String> fallbackVoices;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public SpeechGatewayAdapter(
            @Value("${tts.base-url:http://localhost:8001}") String baseUrl,
            @Value("${tts.voices:maichi}") String configuredVoices,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.fallbackVoices = Arrays.stream(configuredVoices.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("minh") && !s.equalsIgnoreCase("huong") && !s.equalsIgnoreCase("en-default"))
                .distinct()
                .toList();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(2000))
                .build();
    }

    public SpeechGatewayAdapter(String baseUrl, String configuredVoices) {
        this(baseUrl, configuredVoices, new ObjectMapper());
    }

    @Override
    public List<String> getSupportedLocales() {
        return List.of("vi-VN");
    }

    @Override
    public List<String> getSupportedVoices(String locale) {
        if (locale == null || !"vi-VN".equalsIgnoreCase(locale.trim())) {
            return List.of();
        }
        List<String> remoteVoices = fetchRemoteVoices();
        if (!remoteVoices.isEmpty()) {
            return remoteVoices;
        }
        return fallbackVoices.isEmpty() ? List.of("maichi") : fallbackVoices;
    }

    @Override
    public boolean isVoiceSupported(String locale, String voice) {
        if (locale == null || voice == null) {
            return false;
        }
        return getSupportedVoices(locale).contains(voice.trim());
    }

    @Override
    public boolean isSpeedSupported() {
        return false;
    }

    @Override
    public boolean isPitchSupported() {
        return false;
    }

    @Override
    public boolean isProviderAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofMillis(2000))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                return false;
            }
            return objectMapper.readTree(body).path("engine_ready").asBoolean(false);
        } catch (Exception e) {
            log.debug("TTS provider unavailable at {}: {}", baseUrl, e.getMessage());
            return false;
        }
    }

    private List<String> fetchRemoteVoices() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/voices"))
                    .timeout(Duration.ofMillis(2000))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode voicesNode = root.get("voices");
                if (voicesNode != null && voicesNode.isArray()) {
                    List<String> voices = new ArrayList<>();
                    for (JsonNode v : voicesNode) {
                        String voiceName = v.asText();
                        if (voiceName != null && !voiceName.isBlank()) {
                            voices.add(voiceName.trim());
                        }
                    }
                    if (!voices.isEmpty()) {
                        return voices;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch remote voices from TTS service at {}: {}", baseUrl, e.getMessage());
        }
        return List.of();
    }
}
