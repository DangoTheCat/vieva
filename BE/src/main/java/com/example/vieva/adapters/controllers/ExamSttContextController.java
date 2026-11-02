package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.speech.ExamSttContextQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/stt")
public class ExamSttContextController {
    private final ExamSttContextQuery contexts;
    private final String secret;
    public ExamSttContextController(ExamSttContextQuery contexts,
            @Value("${STT_CONTEXT_SECRET:}") String secret) {
        this.contexts = contexts; this.secret = secret;
    }
    @GetMapping("/attempts/{attemptId}/context")
    public Map<String, String> context(@PathVariable UUID attemptId,
            @AuthenticationPrincipal ActorView actor,
            @RequestHeader(value="X-STT-Context-Secret", defaultValue="") String supplied) {
        if (secret.isBlank() || !MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return Map.of("initial_prompt", contexts.forAttempt(attemptId, CurrentUser.id(actor)));
    }
}
