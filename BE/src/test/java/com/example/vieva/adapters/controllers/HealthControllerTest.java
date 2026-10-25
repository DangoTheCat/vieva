package com.example.vieva.adapters.controllers;

import com.example.vieva.application.ports.input.health.ReadinessCheckUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReadinessCheckUseCase readinessCheckUseCase;

    @BeforeEach
    void setUp() {
        HealthController controller = new HealthController(readinessCheckUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/health returns status UP and service vieva-api without authentication")
    void healthReturnsStatusUpAndService() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("vieva-api"));
    }

    @Test
    @DisplayName("GET /api/v1/health/readiness returns 200 and database UP when ready")
    void readinessReturnsOkWhenReady() throws Exception {
        when(readinessCheckUseCase.isReady()).thenReturn(true);

        mockMvc.perform(get("/api/v1/health/readiness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("vieva-api"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health/readiness returns 503 and database DOWN when not ready")
    void readinessReturnsServiceUnavailableWhenNotReady() throws Exception {
        when(readinessCheckUseCase.isReady()).thenReturn(false);

        mockMvc.perform(get("/api/v1/health/readiness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.service").value("vieva-api"))
                .andExpect(jsonPath("$.database").value("DOWN"));
    }

    @Test
    @DisplayName("GET /api/v1/health/readiness returns 503 when readinessCheckUseCase is null")
    void readinessReturnsServiceUnavailableWhenUseCaseNull() throws Exception {
        MockMvc fallbackMvc = MockMvcBuilders.standaloneSetup(new HealthController(null)).build();

        fallbackMvc.perform(get("/api/v1/health/readiness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.service").value("vieva-api"))
                .andExpect(jsonPath("$.database").value("DOWN"));
    }
}
