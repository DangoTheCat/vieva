package com.example.vieva.application.ports.input.common;

import java.util.Set;
import java.util.UUID;

public record ActorView(UUID id, String email, Set<String> roleCodes, boolean mustChangePassword) {
}
