package com.example.vieva.application.usecases.user;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.user.CreateUserByAdminRequest;
import com.example.vieva.application.ports.input.user.UpdateUserByAdminRequest;
import com.example.vieva.application.ports.input.user.UserResult;
import com.example.vieva.application.ports.output.AuditEventRepository;
import com.example.vieva.application.ports.output.DomainEventPublisherPort;
import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.Role;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private JsonSerializerPort jsonSerializer;
    @Mock private DomainEventPublisherPort domainEventPublisher;
    @Mock private ExamAttemptRepository examAttemptRepository;

    private AdminUserServiceImpl adminUserService;

    private final UUID adminId = UUID.randomUUID();
    private final Role lecturerRole = Role.builder().roleId(2).roleCode("ROLE_LECTURER").build();
    private final Role studentRole = Role.builder().roleId(3).roleCode("ROLE_STUDENT").build();

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(userRepository, roleRepository, passwordEncoder,
                new QuestionBankAuditor(auditEventRepository, jsonSerializer), domainEventPublisher, examAttemptRepository);
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hashed-" + inv.getArgument(0));
        lenient().when(jsonSerializer.serialize(any())).thenReturn("{}");
    }

    @Test
    @DisplayName("createUser: assigns exactly one role, forces password change and publishes the welcome email")
    void createUser_SingleRole_MustChangePassword_EmailPublished() {
        when(roleRepository.findByRoleCode("ROLE_LECTURER")).thenReturn(Optional.of(lecturerRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResult created = adminUserService.createUser(CreateUserByAdminRequest.builder()
                .email("new.lecturer@fpt.edu.vn")
                .password("Temp123")
                .fullName("New Lecturer")
                .roleCode("LECTURER")
                .build(), adminId);

        assertThat(created.getEmail()).isEqualTo("new.lecturer@fpt.edu.vn");
        assertThat(created.getRole()).isEqualTo("ROLE_LECTURER");
        assertThat(created.isMustChangePassword()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User persisted = captor.getValue();
        assertThat(persisted.getUserRoles()).hasSize(1);
        assertThat(persisted.getUserCode()).startsWith("USR-");

        verify(domainEventPublisher).publishAccountCreated("new.lecturer@fpt.edu.vn", "New Lecturer",
                "ROLE_LECTURER", "Temp123");
    }

    @Test
    @DisplayName("createUser: rejects invalid or retired role codes like ROLE_USER")
    void createUser_InvalidRole_ThrowsRoleNotFound() {
        for (String code : new String[]{"ROLE_USER", "user"}) {
            assertThatThrownBy(() -> adminUserService.createUser(CreateUserByAdminRequest.builder()
                    .email("someone@fpt.edu.vn")
                    .fullName("Someone")
                    .roleCode(code)
                    .build(), adminId))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        }
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("createUser: no role given defaults to ROLE_STUDENT")
    void createUser_NoRole_DefaultsToStudent() {
        when(roleRepository.findByRoleCode("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResult created = adminUserService.createUser(CreateUserByAdminRequest.builder()
                .email("default@fpt.edu.vn")
                .fullName("Default Role")
                .build(), adminId);

        assertThat(created.getRole()).isEqualTo("ROLE_STUDENT");
    }

    @Test
    @DisplayName("createUser: maps DuplicateKeyException with user_code on flush to USER_CODE_EXISTED")
    void createUser_DuplicateKeyUserCode_ThrowsUserCodeExisted() {
        when(roleRepository.findByRoleCode("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(
                new DuplicateKeyException("uq_users_active_user_code", ErrorCode.USER_CODE_EXISTED)
        );

        assertThatThrownBy(() -> adminUserService.createUser(CreateUserByAdminRequest.builder()
                .email("code.duplicate@fpt.edu.vn")
                .fullName("Duplicate Code")
                .build(), adminId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_CODE_EXISTED);
    }

    @Test
    @DisplayName("updateUser: successfully updates email and userCode when not duplicate")
    void updateUser_EmailAndUserCode_Success() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder()
                .userId(userId)
                .email("old@fpt.edu.vn")
                .userCode("OLD-CODE")
                .fullName("Old Name")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@fpt.edu.vn")).thenReturn(false);
        when(userRepository.existsByUserCode("NEW-CODE")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResult result = adminUserService.updateUser(userId, UpdateUserByAdminRequest.builder()
                .email("new@fpt.edu.vn")
                .userCode("NEW-CODE")
                .fullName("Updated Name")
                .build(), adminId);

        assertThat(result.getEmail()).isEqualTo("new@fpt.edu.vn");
        assertThat(existing.getEmail()).isEqualTo("new@fpt.edu.vn");
        assertThat(existing.getUserCode()).isEqualTo("NEW-CODE");
    }

    @Test
    @DisplayName("updateUser: rejects duplicate email with USER_EXISTED")
    void updateUser_DuplicateEmail_ThrowsUserExisted() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder()
                .userId(userId)
                .email("old@fpt.edu.vn")
                .userCode("OLD-CODE")
                .fullName("Old Name")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("existing@fpt.edu.vn")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.updateUser(userId, UpdateUserByAdminRequest.builder()
                .email("existing@fpt.edu.vn")
                .build(), adminId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_EXISTED);
    }

    @Test
    @DisplayName("updateUser: maps DuplicateKeyException with user_code on flush to USER_CODE_EXISTED")
    void updateUser_FlushDuplicateKeyUserCode_ThrowsUserCodeExisted() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder()
                .userId(userId)
                .email("old@fpt.edu.vn")
                .userCode("OLD-CODE")
                .fullName("Old Name")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(
                new DuplicateKeyException("Key (user_code)=(NEW-CODE) already exists", ErrorCode.USER_CODE_EXISTED)
        );

        assertThatThrownBy(() -> adminUserService.updateUser(userId, UpdateUserByAdminRequest.builder()
                .userCode("NEW-CODE")
                .build(), adminId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_CODE_EXISTED);
    }

    @Test
    @DisplayName("deleteUser: rejects deletion of student with existing attempts")
    void deleteUser_StudentWithAttempts_ThrowsCannotDeleteStudentWithAttempts() {
        UUID studentId = UUID.randomUUID();
        User student = User.builder()
                .userId(studentId)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(examAttemptRepository.findByStudentId(studentId)).thenReturn(List.of(mock(ExamAttempt.class)));

        assertThatThrownBy(() -> adminUserService.deleteUser(studentId, adminId))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_DELETE_STUDENT_WITH_ATTEMPTS);
    }
}
