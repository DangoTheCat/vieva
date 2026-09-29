package com.example.vieva.application.ports.output;

import java.util.UUID;

public interface TokenProviderPort {
    String generateToken(UUID userId, String email);
    UUID getUserIdFromToken(String token);
    boolean validateToken(String token);
}
