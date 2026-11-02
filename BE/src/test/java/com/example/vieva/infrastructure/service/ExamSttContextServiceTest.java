package com.example.vieva.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExamSttContextServiceTest {
    @Test void formatsOnlyBoundedUniqueTerms() {
        assertEquals("Thuật ngữ: API, Docker", ExamSttContextService.formatTerms(List.of(" API ", "api", "Docker", " ", "x".repeat(81))));
        assertEquals("", ExamSttContextService.formatTerms(List.of()));
        assertTrue(ExamSttContextService.formatTerms(java.util.stream.IntStream.range(0, 100).mapToObj(i -> "term"+i).toList()).length() < 1215);
    }

    @Test @SuppressWarnings("unchecked") void generatesOnceAfterCommitAndChecksOwnership() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ObjectProvider<ChatModel> models = mock(ObjectProvider.class);
        ChatModel model = mock(ChatModel.class);
        when(models.getIfAvailable()).thenReturn(model);
        var response = ChatResponse.builder().aiMessage(AiMessage.from("{\"terms\":[\"Docker\",\"API\"]}")).build();
        when(model.chat(any(ChatRequest.class))).thenReturn(response);
        UUID exam = UUID.randomUUID(), attempt = UUID.randomUUID(), student = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(exam))).thenReturn(List.of(Map.of("question_content", "Question marker", "reference_answer", "Answer marker")));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(attempt), eq(student))).thenReturn(List.of(exam));
        var service = new ExamSttContextService(jdbc, models, new ObjectMapper(), "gemini");
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.dispatch(exam);
            verifyNoInteractions(model);
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
        String prompt = "";
        for (int i=0; i<100 && prompt.isEmpty(); i++) { prompt = service.forAttempt(attempt, student); Thread.sleep(10); }
        assertEquals("Thuật ngữ: Docker, API", prompt);
        service.forAttempt(attempt, student);
        verify(model, times(1)).chat(argThat((ChatRequest request) -> request.messages().toString().contains("Answer marker") && request.messages().toString().contains("Question marker")));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(attempt), eq(student))).thenReturn(List.of());
        assertEquals("", service.forAttempt(attempt, student));
    }

    @Test @SuppressWarnings("unchecked") void providerFailureDoesNotBreakTheAttemptOrRetryOnEveryLookup() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ObjectProvider<ChatModel> models = mock(ObjectProvider.class);
        ChatModel model = mock(ChatModel.class);
        when(models.getIfAvailable()).thenReturn(model);
        UUID exam = UUID.randomUUID(), attempt = UUID.randomUUID(), student = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(exam))).thenReturn(List.of(Map.of("question_content", "API", "reference_answer", "Docker")));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(attempt), eq(student))).thenReturn(List.of(exam));
        when(model.chat(any(ChatRequest.class))).thenThrow(new IllegalStateException("provider unavailable"));
        var service = new ExamSttContextService(jdbc, models, new ObjectMapper(), "gemini");
        assertEquals("", service.forAttempt(attempt, student));
        verify(model, timeout(2000)).chat(any(ChatRequest.class));
        assertEquals("", service.forAttempt(attempt, student));
        verify(model, times(1)).chat(any(ChatRequest.class));
    }

    @Test @SuppressWarnings("unchecked") void rollbackDoesNotStartGeneration() {
        var jdbc = mock(JdbcTemplate.class);
        ObjectProvider<ChatModel> models = mock(ObjectProvider.class);
        var service = new ExamSttContextService(jdbc, models, new ObjectMapper(), "gemini");
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.dispatch(UUID.randomUUID());
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(1));
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
        verifyNoInteractions(jdbc, models);
    }
}
