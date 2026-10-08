package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.output.TokenProviderPort;
import com.example.vieva.application.usecases.user.UserService;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserRole;
import com.example.vieva.domain.entities.UserStatus;
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
import java.time.temporal.ChronoUnit;
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
    private UserService userService;

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
    private User activeUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userId = UUID.randomUUID();
        Role userRole = Role.builder().roleId(1).roleCode("ROLE_STUDENT").build();
        UserRole ur = UserRole.builder().userId(userId).roleId(1).role(userRole).build();

        activeUser = User.builder()
                .userId(userId)
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .userRoles(Set.of(ur))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal: sets authentication when token is valid and no password change")
    void validToken_SetsAuthentication() throws Exception {
        String token = "valid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(Optional.of(activeUser));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(activeUser);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: rejects token if issued before password was changed")
    void revokedToken_AfterPasswordChange_RejectsAuthentication() throws Exception {
        String token = "old-token";
        Instant passwordChangedAt = Instant.now();
        Instant tokenIssuedAt = passwordChangedAt.minus(1, ChronoUnit.HOURS);

        activeUser.setPasswordChangedAt(passwordChangedAt);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(Optional.of(activeUser));
        when(tokenProvider.getIssuedAtFromToken(token)).thenReturn(tokenIssuedAt);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: accepts token if issued after password was changed")
    void validToken_AfterPasswordChange_SetsAuthentication() throws Exception {
        String token = "new-token";
        Instant passwordChangedAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant tokenIssuedAt = Instant.now();

        activeUser.setPasswordChangedAt(passwordChangedAt);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(Optional.of(activeUser));
        when(tokenProvider.getIssuedAtFromToken(token)).thenReturn(tokenIssuedAt);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: user who must change password is blocked from other endpoints with 403")
    void mustChangePassword_OtherEndpoint_Returns403() throws Exception {
        activeUser.setMustChangePassword(true);
        MockHttpServletRequest httpRequest = bearerRequest("GET", "/api/v1/lecturer/subjects");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        stubValidToken();

        filter.doFilterInternal(httpRequest, httpResponse, filterChain);

        assertThat(httpResponse.getStatus()).isEqualTo(403);
        assertThat(httpResponse.getContentAsString()).contains("1062");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("doFilterInternal: user who must change password can still change it")
    void mustChangePassword_ChangePasswordEndpoint_PassesThrough() throws Exception {
        activeUser.setMustChangePassword(true);
        MockHttpServletRequest httpRequest = bearerRequest("PUT", "/api/v1/users/me/password");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        stubValidToken();

        filter.doFilterInternal(httpRequest, httpResponse, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    private MockHttpServletRequest bearerRequest(String method, String uri) {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest(method, uri);
        httpRequest.addHeader("Authorization", "Bearer valid-token");
        return httpRequest;
    }

    private void stubValidToken() {
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(Optional.of(activeUser));
    }
}
