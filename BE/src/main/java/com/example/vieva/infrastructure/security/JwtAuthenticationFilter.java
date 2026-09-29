package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.output.TokenProviderPort;
import com.example.vieva.application.usecases.user.UserService;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProviderPort tokenProvider;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            try {
                UUID userId = tokenProvider.getUserIdFromToken(token);
                Optional<User> userOptional = userService.getById(userId);

                if (userOptional.isPresent() && userOptional.get().getStatus() == UserStatus.ACTIVE) {
                    User user = userOptional.get();
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
                }
            } catch (Exception e) {
                log.error("Could not set user authentication in security context", e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
