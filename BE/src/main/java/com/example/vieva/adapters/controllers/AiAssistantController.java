package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.AiChatApiRequest;
import com.example.vieva.adapters.presenters.AiAssistantDtos.ChatReplyDto;
import com.example.vieva.adapters.presenters.AiAssistantDtos.ContextSnapshotDto;
import com.example.vieva.application.ports.input.AiChatCommand;
import com.example.vieva.application.usecases.ai.AiChatService;
import com.example.vieva.application.usecases.ai.AiContextSnapshotService;
import com.example.vieva.domain.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI assistant for any signed-in user: grounded chat and the context snapshot it is grounded on.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiChatService aiChatService;
    private final AiContextSnapshotService snapshotService;

    @PostMapping("/assistant/chat")
    public ResponseEntity<ChatReplyDto> chat(@Valid @RequestBody AiChatApiRequest request,
                                             @AuthenticationPrincipal User currentUser) {
        AiChatService.ChatReply reply = aiChatService.chat(
                new AiChatCommand(CurrentUser.id(currentUser), request.message()));
        return ResponseEntity.ok(new ChatReplyDto(reply.reply(), reply.snapshotGeneratedAt()));
    }

    @GetMapping("/context-snapshot")
    public ResponseEntity<ContextSnapshotDto> contextSnapshot() {
        AiContextSnapshotService.CachedSnapshot snapshot = snapshotService.current();
        return ResponseEntity.ok(new ContextSnapshotDto(snapshot.content(), snapshot.generatedAt()));
    }
}
