package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.ChangePasswordRequest;
import com.example.vieva.application.ports.input.UpdateProfileRequest;
import com.example.vieva.application.ports.output.PasswordEncoderPort;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .userId(userId)
                .email("test@example.com")
                .userCode("USR-TEST01")
                .fullName("Nguyen Van A")
                .passwordHash("hashed-old-password")
                .phoneNumber("0912345678")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("updateProfile: successfully updates both fullName and phoneNumber")
    void updateProfile_AllFields_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van B")
                .phoneNumber("0987654321")
                .build();

        User result = userService.updateProfile(userId, request);

        assertThat(result.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(result.getPhoneNumber()).isEqualTo("0987654321");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updateProfile: partial update keeps existing phoneNumber when phoneNumber in request is null")
    void updateProfile_OnlyFullName_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van C")
                .phoneNumber(null)
                .build();

        User result = userService.updateProfile(userId, request);

        assertThat(result.getFullName()).isEqualTo("Nguyen Van C");
        assertThat(result.getPhoneNumber()).isEqualTo("0912345678");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updateProfile: partial update keeps existing fullName when fullName in request is null")
    void updateProfile_OnlyPhoneNumber_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName(null)
                .phoneNumber("0999888777")
                .build();

        User result = userService.updateProfile(userId, request);

        assertThat(result.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(result.getPhoneNumber()).isEqualTo("0999888777");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updateProfile: throws USER_NOT_FOUND when user does not exist")
    void updateProfile_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van B")
                .build();

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile: throws INVALID_REQUEST when fullName is empty after trim")
    void updateProfile_BlankFullName_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("    ")
                .build();

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword: successfully changes password when old password matches")
    void changePassword_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPass123", "hashed-old-password")).thenReturn(true);
        when(passwordEncoder.matches("newPass456", "hashed-old-password")).thenReturn(false);
        when(passwordEncoder.encode("newPass456")).thenReturn("hashed-new-password");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass123")
                .newPassword("newPass456")
                .build();

        userService.changePassword(userId, request);

        assertThat(sampleUser.getPasswordHash()).isEqualTo("hashed-new-password");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("changePassword: throws INCORRECT_PASSWORD when old password does not match")
    void changePassword_IncorrectOldPassword_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongOldPass", "hashed-old-password")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrongOldPass")
                .newPassword("newPass456")
                .build();

        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INCORRECT_PASSWORD));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword: throws PASSWORD_UNCHANGED when new password is identical to old password")
    void changePassword_NewPasswordSameAsOld_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("samePassword123", "hashed-old-password")).thenReturn(true);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("samePassword123")
                .newPassword("samePassword123")
                .build();

        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.PASSWORD_UNCHANGED));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword: throws USER_NOT_FOUND when user does not exist")
    void changePassword_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass123")
                .newPassword("newPass456")
                .build();

        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllUsers: returns all users from repository")
    void getAllUsers_Success() {
        User user2 = User.builder()
                .userId(UUID.randomUUID())
                .email("test2@example.com")
                .build();
        when(userRepository.findAll()).thenReturn(List.of(sampleUser, user2));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(sampleUser, user2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getAllUsers: returns empty list when no users exist")
    void getAllUsers_Empty() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<User> result = userService.getAllUsers();

        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }
}
