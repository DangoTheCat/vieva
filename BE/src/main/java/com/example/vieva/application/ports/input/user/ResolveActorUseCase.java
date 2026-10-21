package com.example.vieva.application.ports.input.user;

import com.example.vieva.application.ports.input.common.ActorView;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ResolveActorUseCase {
    Optional<ActorView> resolve(UUID userId, Instant tokenIssuedAt);
}
