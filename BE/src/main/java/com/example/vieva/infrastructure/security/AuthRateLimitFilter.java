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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final int CAPACITY = 10;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    /** Max number of distinct IPs tracked; oldest entries are evicted when exceeded. */
    private static final int MAX_BUCKETS = 50_000;

    /**
     * Comma-separated list of trusted proxy IPs/CIDRs that may set X-Forwarded-For.
     * Default: only loopback (suitable for local dev). In production set to your
     * load-balancer/nginx IP, e.g.: rate-limit.trusted-proxies=10.0.0.1,10.0.0.2
     */
    @Value("${rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}")
    private Set<String> trustedProxies;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = getClientIp(request);
        // Evict one entry if full before inserting a new key to avoid IllegalStateException in compute
        if (!buckets.containsKey(ip) && buckets.size() >= MAX_BUCKETS) {
            var it = buckets.keySet().iterator();
            if (it.hasNext()) {
                buckets.remove(it.next());
            }
        }
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"code\":\"4290\",\"message\":\"Too many requests \u2014 please try again later.\"}");
        }
    }

    /**
     * Returns the real client IP. X-Forwarded-For is only trusted when the TCP
     * connection originates from a known proxy IP — prevents spoofing via
     * a crafted header from an arbitrary client.
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // XFF format: "client, proxy1, proxy2" — take the leftmost (client) IP
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.greedy(CAPACITY, REFILL_PERIOD));
        return Bucket.builder().addLimit(limit).build();
    }
}

