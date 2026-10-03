package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqChatGatewayTest {

    private static final String URL = "http://groq.test/chat/completions";

    private MockRestServiceServer server;
    private GroqChatGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://groq.test");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new GroqChatGateway(builder.build(), "test-model", 2, Duration.ZERO);
    }

    private static String reply(String content) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + content + "\"}}]}";
    }

    @Test
    @DisplayName("sends an OpenAI-style request and strips <think> from the reply")
    void stripsThinkTags() {
        server.expect(once(), requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("Hỏi"))
                .andExpect(jsonPath("$.max_tokens").value(256))
                .andRespond(withSuccess(reply("<think>nghĩ thầm</think>\\nTrả lời."), MediaType.APPLICATION_JSON));

        assertThat(gateway.chat("Hệ thống", "Hỏi", 0.4, 256)).isEqualTo("Trả lời.");
        server.verify();
    }

    @Test
    @DisplayName("retries 429 and 5xx, then succeeds")
    void retriesTransientErrors() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(once(), requestTo(URL)).andRespond(withSuccess(reply("OK"), MediaType.APPLICATION_JSON));

        assertThat(gateway.chat("s", "u", 0.4, 256)).isEqualTo("OK");
        server.verify();
    }

    @Test
    @DisplayName("gives up after max retries")
    void givesUpAfterMaxRetries() {
        server.expect(times(3), requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> gateway.chat("s", "u", 0.4, 256)).isInstanceOf(AiServiceException.class);
        server.verify();
    }

    @Test
    @DisplayName("does not retry other 4xx errors")
    void noRetryOnClientError() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> gateway.chat("s", "u", 0.4, 256)).isInstanceOf(AiServiceException.class);
        server.verify();
    }

    @Test
    @DisplayName("reasoning-only reply counts as empty")
    void reasoningOnlyReply() {
        server.expect(once(), requestTo(URL))
                .andRespond(withSuccess(reply("<think>chưa xong"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.chat("s", "u", 0.4, 256)).isInstanceOf(AiServiceException.class);
    }

    @Test
    @DisplayName("stripReasoning removes closed and unclosed reasoning tags")
    void stripReasoning() {
        assertThat(GroqChatGateway.stripReasoning("<THINKING>a</THINKING>Kết quả<reasoning>b</reasoning>"))
                .isEqualTo("Kết quả");
        assertThat(GroqChatGateway.stripReasoning("Kết quả <think>cắt ngang")).isEqualTo("Kết quả");
    }
}
