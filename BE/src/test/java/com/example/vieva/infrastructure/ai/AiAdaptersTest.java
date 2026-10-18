package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.example.vieva.domain.entities.BloomLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiAdaptersTest {

    private static final QuestionGenerationPrompt PROMPT = new QuestionGenerationPrompt("CSDL", "Giao dịch",
            Map.of(BloomLevel.APPLY, 1, BloomLevel.CREATE, 1), "Tập trung ACID",
            List.of(new QuestionGenerationPrompt.ContextChunk("C1", "csdl.pdf", 3,
                    "Giao dịch có bốn tính chất ACID. Tính nguyên tử bảo đảm tất cả hoặc không gì cả.")),
            List.of("Câu cũ đã có"), null, null);

    private static ChatModel replying(String text) {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder().aiMessage(AiMessage.from(text)).build());
        return model;
    }

    @Test
    @DisplayName("OpenAI adapter parses the fixed JSON schema even inside a markdown fence")
    void parsesSchema() {
        String reply = """
                ```json
                {"questions":[{"content":"Áp dụng ACID?","expectedAnswer":"...","bloomLevel":"APPLY",
                  "rubric":{"totalScore":10,"criteria":[{"name":"A","description":"d","maxScore":10}]},
                  "evidenceIds":["C1:E1"],"extra":"ignored"}]}
                ```""";
        List<GeneratedQuestionCandidate> candidates =
                new GeminiLLMAdapter(replying(reply), new ObjectMapper()).generate(PROMPT);
        assertThat(candidates).singleElement().satisfies(c -> {
            assertThat(c.bloomLevel()).isEqualTo("APPLY");
            assertThat(c.rubricTotalScore()).isEqualByComparingTo("10");
            assertThat(c.sourceChunkRefs()).containsExactly("C1");
            assertThat(c.citations()).singleElement().satisfies(q -> assertThat(q.quote()).isEqualTo("Giao dịch có bốn tính chất ACID."));
        });
    }

    @Test
    void wrapsProviderAndFormatErrors() {
        ChatModel failing = mock(ChatModel.class);
        when(failing.chat(any(ChatRequest.class))).thenThrow(new RuntimeException("401 invalid api key sk-secret"));
        assertThatThrownBy(() -> new GeminiLLMAdapter(failing, new ObjectMapper()).generate(PROMPT))
                .isInstanceOf(AiServiceException.class)
                .hasMessageNotContaining("sk-secret");
        assertThatThrownBy(() -> new GeminiLLMAdapter(replying("not json"), new ObjectMapper()).generate(PROMPT))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void rejectsUnknownEvidenceAndNullPayload() {
        for(String reply:List.of("null","{\"questions\":[{\"evidenceIds\":[\"C99:E1\"]}]}")) {
            assertThatThrownBy(() -> new GeminiLLMAdapter(replying(reply),new ObjectMapper()).generate(PROMPT))
                    .isInstanceOf(AiServiceException.class);
        }
    }

    @Test
    void sendsSchemaOverHttpAndResolvesExactQuote() throws Exception {
        var mapper=new ObjectMapper();
        var received=new java.util.concurrent.atomic.AtomicReference<String>();
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/chat/completions", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            String payload="{\"questions\":[{\"content\":\"ACID?\",\"expectedAnswer\":\"...\",\"bloomLevel\":\"APPLY\",\"evidenceIds\":[\"C1:E2\"]}]}";
            byte[] response=mapper.writeValueAsBytes(Map.of("choices",List.of(Map.of("index",0,"finish_reason","stop",
                    "message",Map.of("role","assistant","content",payload)))));
            exchange.getResponseHeaders().add("Content-Type","application/json");
            exchange.sendResponseHeaders(200,response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var model=dev.langchain4j.model.openai.OpenAiChatModel.builder().apiKey("test-key").modelName("test")
                    .baseUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/v1").strictJsonSchema(true)
                    .maxRetries(0).timeout(java.time.Duration.ofSeconds(5)).build();
            var candidates=new GeminiLLMAdapter(model,mapper).generateQuestionAndRubric(PROMPT);
            assertThat(candidates.get(0).citations().get(0).quote()).isEqualTo("Tính nguyên tử bảo đảm tất cả hoặc không gì cả.");
            var format=mapper.readTree(received.get()).get("response_format");
            assertThat(format.get("type").asText()).isEqualTo("json_schema");
            assertThat(format.at("/json_schema/strict").asBoolean()).isTrue();
            assertThat(format.at("/json_schema/schema/properties/questions/items/properties/evidenceIds/items/enum").toString())
                    .contains("C1:E1","C1:E2");
        } finally { server.stop(0); }
    }

    @Test
    @DisplayName("prompt states the 6 Bloom levels, the per-level counts, context refs and avoid list")
    void promptContent() {
        String user = QuestionPromptBuilder.userMessage(PROMPT);
        assertThat(QuestionPromptBuilder.systemMessage())
                .contains("REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE")
                .contains("MUST equal the sum");
        assertThat(user).contains("APPLY=1").contains("CREATE=1").contains("[C1] (document: csdl.pdf, page 3)")
                .contains("- Câu cũ đã có").contains("Tập trung ACID");
    }

    @Test
    @DisplayName("mock LLM honours the requested Bloom distribution with grounded citations")
    void mockGenerator() {
        List<GeneratedQuestionCandidate> candidates = new MockQuestionGenerationGateway(0.8).generate(PROMPT);
        assertThat(candidates).extracting(GeneratedQuestionCandidate::bloomLevel).containsExactlyInAnyOrder("APPLY", "CREATE");
        assertThat(candidates).allSatisfy(c -> assertThat(PROMPT.contextChunks().get(0).content())
                .contains(c.citations().get(0).quote()));
    }
}
