package com.example.vieva.adapters.presenters;

import java.time.Instant;

public final class AiAssistantDtos {

    private AiAssistantDtos() {
    }

    public record ChatReplyDto(String reply, Instant snapshotGeneratedAt) {
    }

    public record ContextSnapshotDto(String content, Instant generatedAt) {
    }
}
