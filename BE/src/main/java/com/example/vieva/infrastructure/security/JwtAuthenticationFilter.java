package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.output.TokenProviderPort;
import com.example.vieva.application.usecases.user.UserService;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.infrastructure.web.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Endpoints a user who must change their password can still call: read own profile, change password. */
    private static final Set<String> PASSWORD_CHANGE_ALLOWED = Set.of(
            "GET /api/v1/users/me",
            "PUT /api/v1/users/me/password");

    private final TokenProviderPort tokenProvider;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        boolean passwordChangeRequired = false;

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            try {
                UUID userId = tokenProvider.getUserIdFromToken(token);
                Optional<User> userOptional = userService.getById(userId);

                if (userOptional.isPresent() && userOptional.get().getStatus() == UserStatus.ACTIVE) {
                    User user = userOptional.get();

                    // Check if token was issued before the user changed password (revocation)
                    if (user.getPasswordChangedAt() != null) {
                        Instant tokenIssuedAt = tokenProvider.getIssuedAtFromToken(token);
                        if (tokenIssuedAt == null || tokenIssuedAt.isBefore(user.getPasswordChangedAt().truncatedTo(ChronoUnit.SECONDS))) {
                            log.warn("JWT token rejected: issued before user {} changed password (revoked)", userId);
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }

                    Set<SimpleGrantedAuthority> authorities = user.getUserRoles() != null
                            ? user.getUserRoles().stream()
                            .filter(ur -> ur.getRole() != null && StringUtils.hasText(ur.getRole().getRoleCode()))
                            .map(ur -> {
                                String code = ur.getRole().getRoleCode();
                                return new SimpleGrantedAuthority(code.startsWith("ROLE_") ? code : "ROLE_" + code);
                            })
                            .collect(Collectors.toSet())
                            : Set.of();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // First login after an admin created the account (or reset its password):
                    // block everything except changing the password
                    passwordChangeRequired = user.isMustChangePassword() && !isPasswordChangeAllowed(request);
                }
            } catch (Exception e) {
                log.error("Could not set user authentication in security context", e);
            }
        }

        if (passwordChangeRequired) {
            writePasswordChangeRequired(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPasswordChangeAllowed(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return "OPTIONS".equals(request.getMethod())
                || PASSWORD_CHANGE_ALLOWED.contains(request.getMethod() + " " + path);
    }

    private void writePasswordChangeRequired(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.builder()
                .code(ErrorCode.PASSWORD_CHANGE_REQUIRED.getCode())
                .message(ErrorCode.PASSWORD_CHANGE_REQUIRED.getMessage())
                .build());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
