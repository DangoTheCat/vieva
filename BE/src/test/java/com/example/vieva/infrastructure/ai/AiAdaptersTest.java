package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.AiServiceException;
import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.example.vieva.domain.entities.BloomLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

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
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
        return model;
    }

    @Test
    @DisplayName("OpenAI adapter parses the fixed JSON schema even inside a markdown fence")
    void parsesSchema() {
        String reply = """
                ```json
                {"questions":[{"content":"Áp dụng ACID?","expectedAnswer":"...","bloomLevel":"APPLY",
                  "rubric":{"totalScore":10,"criteria":[{"name":"A","description":"d","maxScore":10}]},
                  "sourceChunkIds":["C1"],"citations":[{"chunkId":"C1","quote":"bốn tính chất ACID"}],"extra":"ignored"}]}
                ```""";
        List<GeneratedQuestionCandidate> candidates =
                new OpenAiQuestionGenerationGateway(replying(reply), new ObjectMapper()).generate(PROMPT);
        assertThat(candidates).singleElement().satisfies(c -> {
            assertThat(c.bloomLevel()).isEqualTo("APPLY");
            assertThat(c.rubricTotalScore()).isEqualByComparingTo("10");
            assertThat(c.sourceChunkRefs()).containsExactly("C1");
            assertThat(c.citations()).singleElement().satisfies(q -> assertThat(q.quote()).isEqualTo("bốn tính chất ACID"));
        });
    }

    @Test
    void wrapsProviderAndFormatErrors() {
        ChatModel failing = mock(ChatModel.class);
        when(failing.call(any(Prompt.class))).thenThrow(new RuntimeException("401 invalid api key sk-secret"));
        assertThatThrownBy(() -> new OpenAiQuestionGenerationGateway(failing, new ObjectMapper()).generate(PROMPT))
                .isInstanceOf(AiServiceException.class)
                .hasMessageNotContaining("sk-secret");
        assertThatThrownBy(() -> new OpenAiQuestionGenerationGateway(replying("not json"), new ObjectMapper()).generate(PROMPT))
                .isInstanceOf(AiServiceException.class);
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
        List<GeneratedQuestionCandidate> candidates = new MockQuestionGenerationGateway().generate(PROMPT);
        assertThat(candidates).extracting(GeneratedQuestionCandidate::bloomLevel).containsExactlyInAnyOrder("APPLY", "CREATE");
        assertThat(candidates).allSatisfy(c -> assertThat(PROMPT.contextChunks().get(0).content())
                .contains(c.citations().get(0).quote()));
    }
}
