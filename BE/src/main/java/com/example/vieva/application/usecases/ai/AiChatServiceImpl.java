package com.example.vieva.application.usecases.ai;

import com.example.vieva.application.ports.input.AiChatCommand;
import com.example.vieva.application.ports.output.AiChatPort;
import com.example.vieva.application.ports.output.AiRuleRepository;
import com.example.vieva.domain.entities.AiRule;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Prepends the latest snapshot to the STUDENT_ASSISTANT system prompt and calls the chat gateway
 * with the rule's temperature and token limit.
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    static final String ASSISTANT_RULE_CODE = "STUDENT_ASSISTANT";

    private final AiContextSnapshotService snapshotService;
    private final AiRuleRepository aiRuleRepository;
    private final AiChatPort aiChatPort;

    @Override
    public ChatReply chat(AiChatCommand command) {
        AiRule rule = aiRuleRepository.findActiveByCode(ASSISTANT_RULE_CODE)
                .orElseThrow(() -> new AppException(ErrorCode.AI_RULE_NOT_FOUND));
        AiContextSnapshotService.CachedSnapshot snapshot = snapshotService.current();

        String systemPrompt = snapshot.content() + "\n\n" + rule.getPromptContent();
        String reply = aiChatPort.chat(systemPrompt, command.message().strip(),
                rule.getTemperature().doubleValue(), rule.getMaxTokens());
        return new ChatReply(reply, snapshot.generatedAt());
    }
}
