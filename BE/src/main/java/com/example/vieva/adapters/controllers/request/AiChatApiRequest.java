package com.example.vieva.adapters.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatApiRequest(
        @NotBlank @Size(max = 2000) String message) {
}
