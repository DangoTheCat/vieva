package com.example.vieva.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limiting filter for AI-costly endpoints (LLM chat, question generation,
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

    /** POST prefixes whose requests consume AI budget (chat, generate, retry, import, upload). */
    private static final List<String> AI_POST_PREFIXES = List.of(
            "/api/v1/ai/assistant/chat",
            "/api/v1/lecturer/question-generation-requests",
            "/api/v1/lecturer/question-versions/",
            "/api/v1/lecturer/subjects/",
            "/api/v1/lecturer/documents/");

    /** Max number of distinct IPs tracked; oldest entries are evicted when exceeded. */
    private static final int MAX_BUCKETS = 50_000;

    @Value("${ai.rate-limit.capacity:30}")
    private int capacity;

    @Value("${ai.rate-limit.refill-minutes:1}")
    private long refillMinutes;

    @Value("${rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}")
    private Set<String> trustedProxies;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Serializes the evict-then-insert path so a bucket created by one thread can never be
     * evicted by another thread's size check (which would reset that IP's token count).
     * The hot path (existing IP) stays lock-free.
     */
    private final Object bucketCreateLock = new Object();

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
        String ip = getClientIp(request);
        Bucket bucket = buckets.get(ip);
        if (bucket == null) {
            synchronized (bucketCreateLock) {
                bucket = buckets.get(ip);
                if (bucket == null) {
                    if (buckets.size() >= MAX_BUCKETS) {
                        var it = buckets.keySet().iterator();
                        if (it.hasNext()) {
                            buckets.remove(it.next());
                        }
                    }
                    bucket = buckets.computeIfAbsent(ip, k -> newBucket());
                }
            }
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"code\":\"4291\",\"message\":\"AI request limit exceeded \\u2014 please try again later.\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    private Bucket newBucket() {
        int effectiveCapacity = Math.max(1, capacity);
        long effectivePeriod = Math.max(1, refillMinutes);
        Bandwidth limit = Bandwidth.classic(effectiveCapacity,
                Refill.greedy(effectiveCapacity, Duration.ofMinutes(effectivePeriod)));
        return Bucket.builder().addLimit(limit).build();
    }
}
