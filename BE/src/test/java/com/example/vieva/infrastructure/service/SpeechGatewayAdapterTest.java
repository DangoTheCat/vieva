package com.example.vieva.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpeechGatewayAdapterTest {

    @Test
    @DisplayName("Filters out fake voices and defaults to maichi")
    void filtersOutFakeVoicesAndDefaultsToMaichi() {
        // Given configured voices containing fake voices
        String configuredVoices = "maichi,minh,huong,en-default";
        SpeechGatewayAdapter adapter = new SpeechGatewayAdapter("http://localhost:8001", configuredVoices, new ObjectMapper());

        List<String> locales = adapter.getSupportedLocales();
        assertThat(locales).containsExactly("vi-VN");

        List<String> voices = adapter.getSupportedVoices("vi-VN");
        assertThat(voices).contains("maichi");
        assertThat(voices).doesNotContain("minh", "huong", "en-default");

        assertThat(adapter.isVoiceSupported("vi-VN", "maichi")).isTrue();
        assertThat(adapter.isVoiceSupported("vi-VN", "minh")).isFalse();
        assertThat(adapter.isVoiceSupported("vi-VN", "huong")).isFalse();
        assertThat(adapter.isVoiceSupported("en-US", "en-default")).isFalse();
    }

    @Test
    @DisplayName("Capability checks: speed and pitch modification are unsupported")
    void speedAndPitchAreUnsupported() {
        SpeechGatewayAdapter adapter = new SpeechGatewayAdapter("http://localhost:8001", "maichi", new ObjectMapper());

        assertThat(adapter.isSpeedSupported()).isFalse();
        assertThat(adapter.isPitchSupported()).isFalse();
    }

    @Test
    @DisplayName("Empty configured voices fallback to maichi")
    void emptyConfiguredVoicesFallbackToMaichi() {
        SpeechGatewayAdapter adapter = new SpeechGatewayAdapter("http://localhost:65432", "  ", new ObjectMapper());

        List<String> voices = adapter.getSupportedVoices("vi-VN");
        assertThat(voices).containsExactly("maichi");
    }

    @Test
    @DisplayName("isProviderAvailable parses JSON engine_ready accurately")
    void isProviderAvailableWithJson() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        java.util.concurrent.atomic.AtomicReference<String> responseBody = new java.util.concurrent.atomic.AtomicReference<>("{\"engine_ready\": true}");
        java.util.concurrent.atomic.AtomicInteger statusCode = new java.util.concurrent.atomic.AtomicInteger(200);

        server.createContext("/health", exchange -> {
            byte[] bytes = responseBody.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode.get(), bytes.length);
            try (java.io.OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        try {
            SpeechGatewayAdapter adapter = new SpeechGatewayAdapter("http://localhost:" + port, "maichi", new ObjectMapper());

            // 1. engine_ready: true -> true
            assertThat(adapter.isProviderAvailable()).isTrue();

            // 2. engine_ready: false -> false
            responseBody.set("{\"engine_ready\": false}");
            assertThat(adapter.isProviderAvailable()).isFalse();

            // 3. missing engine_ready property -> false
            responseBody.set("{\"status\": \"ok\"}");
            assertThat(adapter.isProviderAvailable()).isFalse();

            // 4. non-200 status -> false
            statusCode.set(503);
            responseBody.set("{\"engine_ready\": true}");
            assertThat(adapter.isProviderAvailable()).isFalse();
        } finally {
            server.stop(0);
        }
    }
}
