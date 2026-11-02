package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.speech.ExamSttContextQuery;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExamSttContextControllerTest {
    @Test void requiresServiceSecretAndPassesAuthenticatedOwner() {
        var query = mock(ExamSttContextQuery.class);
        var controller = new ExamSttContextController(query, "shared-secret");
        UUID student = UUID.randomUUID(), attempt = UUID.randomUUID();
        var actor = new ActorView(student, "student@example.test", Set.of("STUDENT"), false);
        assertThrows(ResponseStatusException.class, () -> controller.context(attempt, actor, "wrong"));
        verifyNoInteractions(query);
        when(query.forAttempt(attempt, student)).thenReturn("API");
        assertEquals("API", controller.context(attempt, actor, "shared-secret").get("initial_prompt"));
        verify(query).forAttempt(attempt, student);
        assertThrows(ResponseStatusException.class, () -> new ExamSttContextController(query, "").context(attempt, actor, ""));
    }
}
