package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.output.TokenProviderPort;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider implements TokenProviderPort {

    /** Injected from ${JWT_SECRET} env var — no fallback; fail-fast on startup if missing. */
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-hours:1}")
    private long expirationHours;

    @Value("${jwt.issuer:vieva}")
    private String jwtIssuer;

    /**
     * Fail-fast: if the secret is blank the application cannot start safely.
     * This prevents the service from booting without a proper signing key.
     */
    @PostConstruct
    public void validateConfig() {
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET env var is missing or too short (must be ≥ 32 chars). " +
                    "Generate one with: openssl rand -base64 64");
        }
    }

    @Override
    public String generateToken(UUID userId, String email) {
        try {
            JWSSigner signer = new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Instant now = Instant.now();
            Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .issuer(jwtIssuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .claim("email", email)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Could not generate JWT token", e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtSecret.getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return false;
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || !expirationTime.after(new Date())) {
                log.warn("JWT token is expired or has no expiry");
                return false;
            }
            // Validate issuer to prevent tokens signed by a foreign key from being accepted
            if (!jwtIssuer.equals(claims.getIssuer())) {
                log.warn("JWT issuer mismatch: expected={}, got={}", jwtIssuer, claims.getIssuer());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return UUID.fromString(signedJWT.getJWTClaimsSet().getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot extract userId from token", e);
        }
    }

    @Override
    public Instant getIssuedAtFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date issueTime = signedJWT.getJWTClaimsSet().getIssueTime();
            return issueTime != null ? issueTime.toInstant() : null;
        } catch (Exception e) {
            log.error("Cannot extract issue time from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public long getExpirationInSeconds() {
        return expirationHours * 3600;
    }
}

