package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiChatPort;
import com.example.vieva.application.ports.output.AiServiceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Chat through Groq's OpenAI-compatible {@code /chat/completions} API
 * ({@code vieva.ai.chat.provider=groq}). Retries 429, 5xx and I/O errors with exponential backoff and
 * removes reasoning tags ({@code <think>} etc.) from the reply. The API key and message contents are
 * never logged.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vieva.ai.chat.provider", havingValue = "groq")
public class GroqChatGateway implements AiChatPort {

    private static final Pattern CLOSED_REASONING =
            Pattern.compile("<(think|thinking|reasoning)>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /** A reply cut by max_tokens can end inside an unclosed reasoning tag. */
    private static final Pattern UNCLOSED_REASONING =
            Pattern.compile("<(think|thinking|reasoning)>.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final RestClient restClient;
    private final String model;
    private final int maxRetries;
    private final Duration baseBackoff;

    @Autowired
    public GroqChatGateway(
            RestClient.Builder restClientBuilder,
            @Value("${vieva.ai.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${vieva.ai.groq.api-key:}") String apiKey,
            @Value("${vieva.ai.groq.model:llama-3.3-70b-versatile}") String model,
            @Value("${vieva.ai.groq.max-retries:2}") int maxRetries,
            @Value("${vieva.ai.groq.timeout:PT30S}") Duration timeout) {
        this(buildClient(restClientBuilder, baseUrl, apiKey, timeout), model, maxRetries, Duration.ofMillis(500));
    }

    GroqChatGateway(RestClient restClient, String model, int maxRetries, Duration baseBackoff) {
        this.restClient = restClient;
        this.model = model;
        this.maxRetries = maxRetries;
        this.baseBackoff = baseBackoff;
    }

    private static RestClient buildClient(RestClient.Builder builder, String baseUrl, String apiKey, Duration timeout) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("vieva.ai.groq.api-key (GROQ_API_KEY) is required when vieva.ai.chat.provider=groq");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userMessage, double temperature, int maxTokens) {
        ChatRequest request = new ChatRequest(model,
                List.of(new Message("system", systemPrompt), new Message("user", userMessage)),
                temperature, maxTokens);

        for (int attempt = 0; ; attempt++) {
            try {
                ChatResponse response = restClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(ChatResponse.class);
                return extractReply(response);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() != 429 || attempt >= maxRetries) {
                    log.warn("Groq call failed with HTTP {}", e.getStatusCode().value());
                    throw new AiServiceException("LLM provider call failed", e);
                }
                log.info("Groq rate limited, retry {}/{}", attempt + 1, maxRetries);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                if (attempt >= maxRetries) {
                    log.warn("Groq call failed after {} attempts: {}", attempt + 1, e.getClass().getSimpleName());
                    throw new AiServiceException("LLM provider call failed", e);
                }
                log.info("Groq call failed ({}), retry {}/{}", e.getClass().getSimpleName(), attempt + 1, maxRetries);
            } catch (RestClientException e) {
                log.warn("Groq call failed: {}", e.getClass().getSimpleName());
                throw new AiServiceException("LLM provider call failed", e);
            }
            sleep(baseBackoff.multipliedBy(1L << attempt));
        }
    }

    private static String extractReply(ChatResponse response) {
        String raw = response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                ? null : response.choices().get(0).message().content();
        String reply = raw == null ? "" : stripReasoning(raw);
        if (reply.isBlank()) {
            throw new AiServiceException("LLM returned an empty response");
        }
        return reply;
    }

    static String stripReasoning(String raw) {
        String withoutClosed = CLOSED_REASONING.matcher(raw).replaceAll("");
        return UNCLOSED_REASONING.matcher(withoutClosed).replaceAll("").strip();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("LLM call interrupted", e);
        }
    }

    record ChatRequest(String model, List<Message> messages, double temperature,
                       @JsonProperty("max_tokens") int maxTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }
}
