package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.CreateUserByAdminRequest;
import com.example.vieva.application.ports.input.UpdateUserByAdminRequest;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private JsonSerializerPort jsonSerializer;
    @Mock private DomainEventPublisherPort domainEventPublisher;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private final UUID adminId = UUID.randomUUID();
    private final Role lecturerRole = Role.builder().roleId(2).roleCode("ROLE_LECTURER").build();
    private final Role studentRole = Role.builder().roleId(3).roleCode("ROLE_STUDENT").build();

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hashed-" + inv.getArgument(0));
        lenient().when(jsonSerializer.serialize(any())).thenReturn("{}");
    }

    @Test
    @DisplayName("createUser: assigns exactly one role, forces password change and publishes the welcome email")
    void createUser_SingleRole_MustChangePassword_EmailPublished() {
        when(roleRepository.findByRoleCode("ROLE_LECTURER")).thenReturn(Optional.of(lecturerRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = adminUserService.createUser(CreateUserByAdminRequest.builder()
                .email("new.lecturer@fpt.edu.vn")
                .password("Temp123")
                .fullName("New Lecturer")
                .roleCode("LECTURER")
                .build(), adminId);

        assertThat(created.getUserRoles()).hasSize(1);
        assertThat(created.getRole().getRoleCode()).isEqualTo("ROLE_LECTURER");
        assertThat(created.isMustChangePassword()).isTrue();
        verify(domainEventPublisher).publishAccountCreated(
                "new.lecturer@fpt.edu.vn", "New Lecturer", "ROLE_LECTURER", "Temp123");
    }

    @Test
    @DisplayName("createUser: generates a temporary password when the admin leaves it blank")
    void createUser_BlankPassword_GeneratesTemporaryPassword() {
        when(roleRepository.findByRoleCode("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminUserService.createUser(CreateUserByAdminRequest.builder()
                .email("student@fpt.edu.vn")
                .fullName("Student")
                .roleCode("ROLE_STUDENT")
                .build(), adminId);

        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(domainEventPublisher).publishAccountCreated(eq("student@fpt.edu.vn"), eq("Student"),
                eq("ROLE_STUDENT"), password.capture());
        assertThat(password.getValue()).hasSize(12);
        verify(passwordEncoder).encode(password.getValue());
    }

    @Test
    @DisplayName("updateUser: changing the role replaces the old one instead of adding a second")
    void updateUser_ChangeRole_ReplacesExistingRole() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("user@fpt.edu.vn")
                .fullName("User")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        user.assignRole(studentRole, adminId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleCode("ROLE_LECTURER")).thenReturn(Optional.of(lecturerRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = adminUserService.updateUser(userId,
                UpdateUserByAdminRequest.builder().roleCode("ROLE_LECTURER").build(), adminId);

        assertThat(updated.getUserRoles()).hasSize(1);
        assertThat(updated.getRole().getRoleCode()).isEqualTo("ROLE_LECTURER");
    }

    @Test
    @DisplayName("resetPassword: admin-set password must be changed on next login")
    void resetPassword_SetsMustChangePassword() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        adminUserService.resetPassword(userId, "Reset123", adminId);

        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("hashed-Reset123");
    }

    @Test
    @DisplayName("createUser: ROLE_USER (or its alias) cannot be assigned by an admin")
    void createUser_RoleUser_Rejected() {
        for (String code : new String[]{"ROLE_USER", "user"}) {
            assertThatThrownBy(() -> adminUserService.createUser(CreateUserByAdminRequest.builder()
                    .email("someone@fpt.edu.vn")
                    .fullName("Someone")
                    .roleCode(code)
                    .build(), adminId))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROLE_NOT_ASSIGNABLE);
        }
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("createUser: no role given defaults to ROLE_STUDENT")
    void createUser_NoRole_DefaultsToStudent() {
        when(roleRepository.findByRoleCode("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = adminUserService.createUser(CreateUserByAdminRequest.builder()
                .email("default@fpt.edu.vn")
                .fullName("Default Role")
                .build(), adminId);

        assertThat(created.getRole().getRoleCode()).isEqualTo("ROLE_STUDENT");
    }
}
