package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.user.ResolveActorUseCase;
import com.example.vieva.application.ports.output.TokenProviderPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenProviderPort tokenProvider;

    @Mock
    private ResolveActorUseCase resolveActorUseCase;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private UUID userId;
    private ActorView activeActor;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userId = UUID.randomUUID();
        activeActor = new ActorView(userId, "test@example.com", Set.of("ROLE_STUDENT"), false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal: sets authentication when token is valid and resolveActor succeeds")
    void validToken_SetsAuthentication() throws Exception {
        String token = "valid-token";
        Instant tokenIssuedAt = Instant.now();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(tokenProvider.getIssuedAtFromToken(token)).thenReturn(tokenIssuedAt);
        when(resolveActorUseCase.resolve(userId, tokenIssuedAt)).thenReturn(Optional.of(activeActor));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(activeActor);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: rejects token if resolveActor returns empty (e.g. revoked or inactive)")
    void revokedOrInactiveToken_RejectsAuthentication() throws Exception {
        String token = "old-token";
        Instant tokenIssuedAt = Instant.now();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(tokenProvider.getIssuedAtFromToken(token)).thenReturn(tokenIssuedAt);
        when(resolveActorUseCase.resolve(userId, tokenIssuedAt)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: user who must change password is blocked from other endpoints with 403")
    void mustChangePassword_OtherEndpoint_Returns403() throws Exception {
        ActorView mustChangeActor = new ActorView(userId, "test@example.com", Set.of("ROLE_STUDENT"), true);
        MockHttpServletRequest httpRequest = bearerRequest("GET", "/api/v1/lecturer/subjects");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        stubValidToken(mustChangeActor);

        filter.doFilterInternal(httpRequest, httpResponse, filterChain);

        assertThat(httpResponse.getStatus()).isEqualTo(403);
        assertThat(httpResponse.getContentAsString()).contains("1062");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("doFilterInternal: user who must change password can still change it")
    void mustChangePassword_ChangePasswordEndpoint_PassesThrough() throws Exception {
        ActorView mustChangeActor = new ActorView(userId, "test@example.com", Set.of("ROLE_STUDENT"), true);
        MockHttpServletRequest httpRequest = bearerRequest("PUT", "/api/v1/users/me/password");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        stubValidToken(mustChangeActor);

        filter.doFilterInternal(httpRequest, httpResponse, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    @DisplayName("doFilterInternal: user who must change password can still read own profile")
    void mustChangePassword_GetMeEndpoint_PassesThrough() throws Exception {
        ActorView mustChangeActor = new ActorView(userId, "test@example.com", Set.of("ROLE_STUDENT"), true);
        MockHttpServletRequest httpRequest = bearerRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        stubValidToken(mustChangeActor);

        filter.doFilterInternal(httpRequest, httpResponse, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    private MockHttpServletRequest bearerRequest(String method, String uri) {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest(method, uri);
        httpRequest.addHeader("Authorization", "Bearer valid-token");
        return httpRequest;
    }

    private void stubValidToken(ActorView actor) {
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn(userId);
        when(tokenProvider.getIssuedAtFromToken("valid-token")).thenReturn(Instant.now());
        when(resolveActorUseCase.resolve(eq(userId), any())).thenReturn(Optional.of(actor));
    }
}
