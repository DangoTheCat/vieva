package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserRole;
import com.example.vieva.domain.entities.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveActorUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResolveActorUseCaseImpl useCase;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        Role role = Role.builder().roleId(1).roleCode("ROLE_STUDENT").build();
        UserRole userRole = UserRole.builder().userId(userId).roleId(1).role(role).build();

        activeUser = User.builder()
                .userId(userId)
                .email("student@example.com")
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .userRoles(Set.of(userRole))
                .build();
    }

    @Test
    @DisplayName("resolve: returns ActorView for active user with no password change")
    void resolve_activeUser_returnsActorView() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Optional<ActorView> result = useCase.resolve(userId, Instant.now());

        assertThat(result).isPresent();
        ActorView actor = result.get();
        assertThat(actor.id()).isEqualTo(userId);
        assertThat(actor.email()).isEqualTo("student@example.com");
        assertThat(actor.roleCodes()).containsExactly("ROLE_STUDENT");
        assertThat(actor.mustChangePassword()).isFalse();
    }

    @Test
    @DisplayName("resolve: returns empty when userId is null")
    void resolve_nullUserId_returnsEmpty() {
        Optional<ActorView> result = useCase.resolve(null, Instant.now());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolve: returns empty when user is not found")
    void resolve_userNotFound_returnsEmpty() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<ActorView> result = useCase.resolve(userId, Instant.now());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolve: returns empty when user is not ACTIVE")
    void resolve_userNotActive_returnsEmpty() {
        activeUser = activeUser.toBuilder().status(UserStatus.INACTIVE).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Optional<ActorView> result = useCase.resolve(userId, Instant.now());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolve: returns empty when token was issued before password changed")
    void resolve_tokenIssuedBeforePasswordChange_returnsEmpty() {
        Instant passwordChangedAt = Instant.now();
        Instant tokenIssuedAt = passwordChangedAt.minus(1, ChronoUnit.HOURS);
        activeUser = activeUser.toBuilder().passwordChangedAt(passwordChangedAt).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Optional<ActorView> result = useCase.resolve(userId, tokenIssuedAt);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolve: returns empty when password changed but tokenIssuedAt is null")
    void resolve_passwordChangedAndNullTokenIssuedAt_returnsEmpty() {
        activeUser = activeUser.toBuilder().passwordChangedAt(Instant.now()).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Optional<ActorView> result = useCase.resolve(userId, null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resolve: returns ActorView when token was issued after password changed")
    void resolve_tokenIssuedAfterPasswordChange_returnsActorView() {
        Instant passwordChangedAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant tokenIssuedAt = Instant.now();
        activeUser = activeUser.toBuilder().passwordChangedAt(passwordChangedAt).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        Optional<ActorView> result = useCase.resolve(userId, tokenIssuedAt);
        assertThat(result).isPresent();
    }
}
