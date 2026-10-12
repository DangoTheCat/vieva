package com.example.vieva.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Rate-limiting filter for authentication endpoints (/api/v1/auth/**).
 *
 * Strategy: per-IP token bucket — 10 requests per minute per real IP.
 *
 * X-Forwarded-For is only trusted when the direct connection comes from a known
 * trusted proxy (configurable via rate-limit.trusted-proxies). This prevents
 * attackers from sending a spoofed XFF header to bypass the per-IP rate limit.
 *
 * The bucket map is capped at MAX_BUCKETS entries to prevent OOM from IP flooding.
 *
 * NOTE: For multi-node deployments use Redis + Bucket4j-Redis instead of this
 * in-memory implementation.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final IpRateLimiter limiter;

    public AuthRateLimitFilter(@Value("${rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}") Set<String> trustedProxies) {
        limiter = new IpRateLimiter(10, Duration.ofMinutes(1), trustedProxies);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (limiter.tryConsume(request)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"code\":\"4290\",\"message\":\"Too many requests \u2014 please try again later.\"}");
        }
    }
}
