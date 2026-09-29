package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.output.TokenProviderPort;
import com.example.vieva.domain.entities.User;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider implements TokenProviderPort {

    @Value("${jwt.secret:dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLXZpZXZhLXByb2plY3Q=}")
    private String jwtSecret;

    @Value("${jwt.expiration-hours:24}")
    private long expirationHours;

    @Override
    public String generateToken(UUID userId, String email) {
        try {
            JWSSigner signer = new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Instant now = Instant.now();
            Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .issuer("vieva")
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

    public String generateToken(User user) {
        try {
            JWSSigner signer = new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Set<String> roles = user.getUserRoles() != null
                    ? user.getUserRoles().stream()
                    .filter(ur -> ur.getRole() != null)
                    .map(ur -> ur.getRole().getRoleCode())
                    .collect(Collectors.toSet())
                    : Collections.emptySet();

            Instant now = Instant.now();
            Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getUserId().toString())
                    .issuer("vieva")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .claim("email", user.getEmail())
                    .claim("userCode", user.getUserCode())
                    .claim("fullName", user.getFullName())
                    .claim("roles", roles)
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
                return false;
            }
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return expirationTime != null && expirationTime.after(new Date());
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
    public long getExpirationInSeconds() {
        return expirationHours * 3600;
    }
}
