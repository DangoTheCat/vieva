package com.example.vieva.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.Duration;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFiltersTest {

    @Test
    void trustsForwardedIpsOnlyFromConfiguredProxies() {
        IpRateLimiter limiter = new IpRateLimiter(1, Duration.ofHours(1), Set.of("127.0.0.1"));
        assertThat(limiter.tryConsume(request("203.0.113.1", "198.51.100.1"))).isTrue();
        assertThat(limiter.tryConsume(request("203.0.113.1", "198.51.100.2"))).isFalse();
        assertThat(limiter.tryConsume(request("127.0.0.1", "198.51.100.1, 127.0.0.1"))).isTrue();
        assertThat(limiter.tryConsume(request("127.0.0.1", "198.51.100.1"))).isFalse();
        assertThat(limiter.tryConsume(request("127.0.0.1", "198.51.100.2"))).isTrue();
    }

    @Test
    void concurrentRequestsShareOneBucketForTheSameIp() {
        IpRateLimiter limiter = new IpRateLimiter(10, Duration.ofHours(1), Set.of());
        long allowed = IntStream.range(0, 100).parallel()
                .filter(i -> limiter.tryConsume(request("203.0.113.1", null))).count();
        assertThat(allowed).isEqualTo(10);
    }

    @Test
    void filtersKeepIndependentLimitsAndBypassUnrelatedRequests() throws Exception {
        AuthRateLimitFilter auth = new AuthRateLimitFilter(Set.of());
        AiRateLimitFilter ai = new AiRateLimitFilter(1, 60, Set.of());
        for (int i = 0; i < 10; i++) {
            assertThat(apply(auth, "POST", "/api/v1/auth/login").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse deniedAuth = apply(auth, "POST", "/api/v1/auth/login");
        assertThat(deniedAuth.getStatus()).isEqualTo(429);
        assertThat(deniedAuth.getContentAsString()).contains("4290");
        assertThat(apply(ai, "POST", "/api/v1/lecturer/question-generation-requests").getStatus()).isEqualTo(200);
        MockHttpServletResponse deniedAi = apply(ai, "POST", "/api/v1/lecturer/question-generation-requests");
        assertThat(deniedAi.getStatus()).isEqualTo(429);
        assertThat(deniedAi.getContentAsString()).contains("4291");
        assertThat(apply(ai, "GET", "/api/v1/lecturer/question-generation-requests").getStatus()).isEqualTo(200);
        assertThat(apply(auth, "GET", "/api/v1/users/me").getStatus()).isEqualTo(200);
    }

    private static MockHttpServletRequest request(String remoteIp, String forwardedIp) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteIp);
        if (forwardedIp != null) request.addHeader("X-Forwarded-For", forwardedIp);
        return request;
    }

    private static MockHttpServletResponse apply(OncePerRequestFilter filter, String method, String uri) throws Exception {
        MockHttpServletRequest request = request("203.0.113.1", null);
        request.setMethod(method);
        request.setRequestURI(uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {});
        return response;
    }
}
