package com.example.vieva.application.ports.input;

import java.util.UUID;

public record AiChatCommand(UUID userId, String message) {
}
