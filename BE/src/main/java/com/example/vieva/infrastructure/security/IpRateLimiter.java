package com.example.vieva.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** One bounded bucket map per filter; forwarded IPs are accepted only from trusted proxies. */
final class IpRateLimiter {
    private static final int MAX_BUCKETS = 50_000;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Object bucketCreateLock = new Object();
    private final Set<String> trustedProxies;
    private final int capacity;
    private final Duration refillPeriod;

    IpRateLimiter(int capacity, Duration refillPeriod, Set<String> trustedProxies) {
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        this.trustedProxies = trustedProxies;
    }

    boolean tryConsume(HttpServletRequest request) {
        String ip = clientIp(request);
        Bucket bucket = buckets.get(ip);
        if (bucket == null) {
            // Serialize eviction/insertion so another creator cannot reset this IP's token count.
            synchronized (bucketCreateLock) {
                bucket = buckets.get(ip);
                if (bucket == null) {
                    if (buckets.size() >= MAX_BUCKETS) {
                        var it = buckets.keySet().iterator();
                        if (it.hasNext()) buckets.remove(it.next());
                    }
                    bucket = buckets.computeIfAbsent(ip, key -> Bucket.builder()
                            .addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod))).build());
                }
            }
        }
        return bucket.tryConsume(1);
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        }
        return remoteAddr;
    }
}
