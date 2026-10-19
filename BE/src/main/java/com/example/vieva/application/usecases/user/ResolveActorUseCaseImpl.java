package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.user.ResolveActorUseCase;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResolveActorUseCaseImpl implements ResolveActorUseCase {

    private final UserRepository userRepository;

    public ResolveActorUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<ActorView> resolve(UUID userId, Instant tokenIssuedAt) {
        if (userId == null) {
            return Optional.empty();
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            return Optional.empty();
        }

        if (user.getPasswordChangedAt() != null) {
            if (tokenIssuedAt == null || tokenIssuedAt.isBefore(user.getPasswordChangedAt().truncatedTo(ChronoUnit.SECONDS))) {
                return Optional.empty();
            }
        }

        Set<String> roleCodes = user.getUserRoles() != null
                ? user.getUserRoles().stream()
                .filter(ur -> ur.getRole() != null && ur.getRole().getRoleCode() != null && !ur.getRole().getRoleCode().isBlank())
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toSet())
                : Set.of();

        return Optional.of(new ActorView(user.getUserId(), user.getEmail(), roleCodes, user.isMustChangePassword()));
    }
}
