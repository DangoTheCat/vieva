package com.example.vieva.application.usecases.ai;

import com.example.vieva.application.ports.input.AiChatCommand;
import com.example.vieva.application.ports.output.AiChatPort;
import com.example.vieva.application.ports.output.AiRuleRepository;
import com.example.vieva.domain.entities.AiRule;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceImplTest {

    private final AiContextSnapshotService snapshotService = mock(AiContextSnapshotService.class);
    private final AiRuleRepository aiRuleRepository = mock(AiRuleRepository.class);
    private final AiChatPort aiChatPort = mock(AiChatPort.class);
    private final AiChatServiceImpl service = new AiChatServiceImpl(snapshotService, aiRuleRepository, aiChatPort);

    private final Instant generatedAt = Instant.parse("2026-10-03T10:00:00Z");

    @Test
    @DisplayName("system prompt = snapshot then rule prompt; rule temperature and max tokens are used")
    void injectsSnapshotIntoSystemPrompt() {
        when(aiRuleRepository.findActiveByCode("STUDENT_ASSISTANT")).thenReturn(Optional.of(AiRule.builder()
                .ruleCode("STUDENT_ASSISTANT").promptContent("Bạn là AIVES BOT.")
                .temperature(new BigDecimal("0.40")).maxTokens(1024).build()));
        when(snapshotService.current()).thenReturn(new AiContextSnapshotService.CachedSnapshot("SNAPSHOT", generatedAt));
        when(aiChatPort.chat("SNAPSHOT\n\nBạn là AIVES BOT.", "Mất mạng thì sao?", 0.40, 1024))
                .thenReturn("Bạn có 60 giây.");

        AiChatService.ChatReply reply = service.chat(new AiChatCommand(UUID.randomUUID(), "  Mất mạng thì sao? "));

        assertThat(reply.reply()).isEqualTo("Bạn có 60 giây.");
        assertThat(reply.snapshotGeneratedAt()).isEqualTo(generatedAt);
    }

    @Test
    @DisplayName("missing active assistant rule -> AI_RULE_NOT_FOUND, no LLM call")
    void missingRule() {
        when(aiRuleRepository.findActiveByCode("STUDENT_ASSISTANT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.chat(new AiChatCommand(UUID.randomUUID(), "Xin chào")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_RULE_NOT_FOUND);
        verify(aiChatPort, never()).chat(anyString(), anyString(), anyDouble(), anyInt());
    }
}
