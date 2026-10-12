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
import java.util.List;
import java.util.Set;

/**
 * Rate-limiting filter for AI-costly endpoints (question generation,
 * document upload/indexing, bulk import). AI calls cost money and time, so only
 * state-changing requests under these prefixes are throttled; reads stay free.
 *
 * Strategy: per-IP token bucket — {@code ai.rate-limit.capacity} requests per
 * {@code ai.rate-limit.refill-minutes} minutes per real IP (defaults 30/min).
 *
 * X-Forwarded-For is only trusted from known proxies (same rule as
 * {@link AuthRateLimitFilter}). The bucket map is capped to prevent OOM.
 *
 * NOTE: For multi-node deployments use Redis + Bucket4j-Redis instead of this
 * in-memory implementation.
 */
@Component
public class AiRateLimitFilter extends OncePerRequestFilter {

    /** POST prefixes whose requests consume AI budget (generate, retry, import, upload). */
    private static final List<String> AI_POST_PREFIXES = List.of(
            "/api/v1/lecturer/question-generation-requests",
            "/api/v1/lecturer/question-versions/",
            "/api/v1/lecturer/subjects/",
            "/api/v1/lecturer/documents/");

    private final IpRateLimiter limiter;

    public AiRateLimitFilter(@Value("${ai.rate-limit.capacity:30}") int capacity,
                             @Value("${ai.rate-limit.refill-minutes:1}") long refillMinutes,
                             @Value("${rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}") Set<String> trustedProxies) {
        limiter = new IpRateLimiter(Math.max(1, capacity), Duration.ofMinutes(Math.max(1, refillMinutes)), trustedProxies);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String prefix : AI_POST_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return false;
            }
        }
        return true;
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
                    "{\"code\":\"4291\",\"message\":\"AI request limit exceeded \\u2014 please try again later.\"}");
        }
    }
}
