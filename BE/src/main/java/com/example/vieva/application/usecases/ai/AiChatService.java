package com.example.vieva.application.usecases.ai;

import com.example.vieva.application.ports.input.AiChatCommand;

import java.time.Instant;

/**
 * Student-facing AI assistant: one question, one answer, grounded on the latest context snapshot.
 */
public interface AiChatService {

    ChatReply chat(AiChatCommand command);

    record ChatReply(String reply, Instant snapshotGeneratedAt) {
    }
}
