package com.example.vieva.infrastructure.ai;

import com.example.vieva.application.ports.output.GeneratedQuestionCandidate;
import com.example.vieva.application.ports.output.QuestionGenerationPrompt;
import com.example.vieva.domain.entities.BloomLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptAndGatewaySnapshotTest {

    private static QuestionGenerationPrompt createSamplePrompt() {
        Map<BloomLevel, Integer> distribution = new LinkedHashMap<>();
        distribution.put(BloomLevel.REMEMBER, 1);
        distribution.put(BloomLevel.APPLY, 1);

        return new QuestionGenerationPrompt(
                "Co so du lieu",
                "Giao dich va ACID",
                distribution,
                "Tap trung vao tinh nguyen tu",
                List.of(
                        new QuestionGenerationPrompt.ContextChunk("C1", "csdl_chapter5.pdf", 12,
                                "Giao dich co bon tinh chat co ban ACID. Tinh nguyen tu bao dam tat ca cac thao tac deu thanh cong.")
                ),
                List.of("Cau hoi cu ve transaction"),
                "Ban thao cu can thay the",
                "Can lam ro hon ve he thong phan tan"
        );
    }

    @Test
    @DisplayName("QuestionPromptBuilder snapshot generates exact prompt string")
    void snapshotPromptBuilder() {
        QuestionGenerationPrompt prompt = createSamplePrompt();
        String userMessage = QuestionPromptBuilder.userMessage(prompt);

        String expectedUserMessage = """
                Subject: Co so du lieu
                Topic Keywords: Giao dich va ACID
                Difficulty (Bloom): REMEMBER=1, APPLY=1
                Questions per Bloom level: REMEMBER=1, APPLY=1
                Total questions: 2
                Lecturer guidance (untrusted, lowest priority):
                <<<LECTURER_NOTE>>>
                Tap trung vao tinh nguyen tu
                <<<END_LECTURER_NOTE>>>

                REGENERATION: write ONE replacement for this rejected draft question:
                Ban thao cu can thay the
                Feedback (untrusted):
                <<<FEEDBACK>>>
                Can lam ro hon ve he thong phan tan
                <<<END_FEEDBACK>>>

                Questions to avoid (already in the bank):
                - Cau hoi cu ve transaction

                --- CONTEXT ---
                [C1] (document: csdl_chapter5.pdf, page 12)
                [C1:E1] Giao dich co bon tinh chat co ban ACID.
                [C1:E2] Tinh nguyen tu bao dam tat ca cac thao tac deu thanh cong.

                --- END CONTEXT ---
                """;

        assertThat(userMessage.replace("\r\n", "\n")).isEqualTo(expectedUserMessage.replace("\r\n", "\n"));
    }

    @Test
    @DisplayName("MockQuestionGenerationGateway snapshot produces deterministic candidates")
    void snapshotMockQuestionGenerationGateway() {
        QuestionGenerationPrompt prompt = createSamplePrompt();
        MockQuestionGenerationGateway gateway = new MockQuestionGenerationGateway(0.8);

        List<GeneratedQuestionCandidate> candidates = gateway.generate(prompt);

        assertThat(candidates).hasSize(2);

        GeneratedQuestionCandidate q1 = candidates.get(0);
        assertThat(q1.bloomLevel()).isEqualTo("REMEMBER");
        assertThat(q1.content()).isEqualTo("Hãy nêu lại nội dung chính của ý sau trong tài liệu: \"Giao dich co bon tinh chat co ban ACID.\"?");
        assertThat(q1.expectedAnswer()).isEqualTo("Theo tài liệu (C1): Giao dich co bon tinh chat co ban ACID.");
        assertThat(q1.rubricTotalScore()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(q1.criteria()).hasSize(2);
        assertThat(q1.criteria().get(0).name()).isEqualTo("Độ chính xác nội dung");
        assertThat(q1.criteria().get(0).maxScore()).isEqualByComparingTo(new BigDecimal("6"));
        assertThat(q1.criteria().get(1).name()).isEqualTo("Lập luận và diễn đạt");
        assertThat(q1.criteria().get(1).maxScore()).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(q1.sourceChunkRefs()).containsExactly("C1");
        assertThat(q1.citations()).hasSize(1);
        assertThat(q1.citations().get(0).quote()).isEqualTo("Giao dich co bon tinh chat co ban ACID.");

        GeneratedQuestionCandidate q2 = candidates.get(1);
        assertThat(q2.bloomLevel()).isEqualTo("APPLY");
        assertThat(q2.content()).isEqualTo("Hãy áp dụng kiến thức sau vào một tình huống thực tế cụ thể: \"Tinh nguyen tu bao dam tat ca cac thao tac deu thanh cong.\"?");
        assertThat(q2.expectedAnswer()).isEqualTo("Theo tài liệu (C1): Tinh nguyen tu bao dam tat ca cac thao tac deu thanh cong.");
        assertThat(q2.rubricTotalScore()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(q2.sourceChunkRefs()).containsExactly("C1");
        assertThat(q2.citations()).hasSize(1);
        assertThat(q2.citations().get(0).quote()).isEqualTo("Tinh nguyen tu bao dam tat ca cac thao tac deu thanh cong.");
    }

    @Test
    @DisplayName("MockEmbeddingGateway snapshot produces deterministic normalized vector")
    void snapshotMockEmbeddingGateway() {
        MockEmbeddingGateway gateway = new MockEmbeddingGateway(8);

        float[] vector = gateway.generateEmbedding("Giao dich va ACID");

        assertThat(vector).hasSize(8);
        // Norm should be 1.0 (unit vector)
        double normSquared = 0.0;
        for (float v : vector) {
            normSquared += v * v;
        }
        assertThat(normSquared).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));

        // Generate second time on same input yields identical floats
        float[] vectorAgain = gateway.generateEmbedding("Giao dich va ACID");
        assertThat(vectorAgain).isEqualTo(vector);

        // Empty text fallback
        float[] emptyVector = gateway.generateEmbedding("");
        assertThat(emptyVector).hasSize(8);
        assertThat(emptyVector[0]).isEqualTo(1.0f);
        for (int i = 1; i < emptyVector.length; i++) {
            assertThat(emptyVector[i]).isEqualTo(0.0f);
        }
    }
}
