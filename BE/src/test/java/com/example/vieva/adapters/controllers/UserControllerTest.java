package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.controllers.request.ChangePasswordApiRequest;
import com.example.vieva.adapters.controllers.request.UpdateProfileApiRequest;
import com.example.vieva.adapters.presenters.UserDto;
import com.example.vieva.adapters.presenters.UserPresenter;
import com.example.vieva.adapters.web.GlobalExceptionHandler;
import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.common.ActorView;
import com.example.vieva.application.ports.input.user.AdminUserService;
import com.example.vieva.application.ports.input.user.ChangePasswordRequest;
import com.example.vieva.application.ports.input.user.UpdateProfileRequest;
import com.example.vieva.application.ports.input.user.UserResult;
import com.example.vieva.application.ports.input.user.UserService;
import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.usecases.common.ErrorTranslatorImpl;
import com.example.vieva.domain.entities.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private UserPresenter userPresenter;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User authenticatedUser;
    private ActorView authenticatedActor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authenticatedUser = User.builder()
                .userId(UUID.randomUUID())
                .email("student@vieva.edu.vn")
                .fullName("Nguyen Van A")
                .build();
        authenticatedActor = new ActorView(
                authenticatedUser.getUserId(),
                authenticatedUser.getEmail(),
                Set.of("ROLE_STUDENT"),
                false);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && (parameter.getParameterType().equals(ActorView.class)
                            || parameter.getParameterType().equals(User.class));
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                if (webRequest.getHeader("X-Simulate-Anonymous") != null) {
                    return null;
                }
                if (parameter.getParameterType().equals(ActorView.class)) {
                    return authenticatedActor;
                }
                return authenticatedUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler(new ErrorTranslatorImpl()))
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me: updates profile successfully")
    void updateProfile_Success() throws Exception {
        UpdateProfileApiRequest request = UpdateProfileApiRequest.builder()
                .fullName("Nguyen Van A Updated")
                .phoneNumber("0987654321")
                .build();

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        verify(userService).updateProfile(eq(authenticatedUser.getUserId()), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me: returns 401 when user is not authenticated")
    void updateProfile_Unauthenticated() throws Exception {
        UpdateProfileApiRequest request = UpdateProfileApiRequest.builder()
                .fullName("Nguyen Van A")
                .build();

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-Simulate-Anonymous", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("1003"));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me: returns 400 when phoneNumber format is invalid")
    void updateProfile_InvalidPhoneFormat() throws Exception {
        UpdateProfileApiRequest request = UpdateProfileApiRequest.builder()
                .phoneNumber("not-a-valid-phone-12345678901234567890")
                .build();

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me: returns 400 when phoneNumber exceeds 11 digits")
    void updateProfile_PhoneExceeds11Digits_ReturnsBadRequest() throws Exception {
        UpdateProfileApiRequest request = UpdateProfileApiRequest.builder()
                .phoneNumber("012345678901")
                .build();

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me: successfully updates with 11-digit phoneNumber")
    void updateProfile_11DigitsPhone_Success() throws Exception {
        UpdateProfileApiRequest request = UpdateProfileApiRequest.builder()
                .phoneNumber("01234567890")
                .build();

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        verify(userService).updateProfile(eq(authenticatedUser.getUserId()), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me: returns 400 when fullName exceeds 50 characters")
    void updateProfile_FullNameExceeds50Characters_ReturnsBadRequest() throws Exception {
        UpdateProfileApiRequest request = UpdateProfileApiRequest.builder()
                .fullName("A".repeat(51))
                .build();

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/password: changes password successfully")
    void changePassword_Success() throws Exception {
        ChangePasswordApiRequest request = ChangePasswordApiRequest.builder()
                .oldPassword("oldPassword123")
                .newPassword("newPassword456")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));

        verify(userService).changePassword(eq(authenticatedUser.getUserId()), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/password: returns 400 when old password is incorrect")
    void changePassword_IncorrectOldPassword() throws Exception {
        ChangePasswordApiRequest request = ChangePasswordApiRequest.builder()
                .oldPassword("wrongPassword")
                .newPassword("newPassword456")
                .build();

        doThrow(new ApplicationException(ErrorCode.INCORRECT_PASSWORD))
                .when(userService).changePassword(eq(authenticatedUser.getUserId()), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1009"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/password: returns 400 when new password is too short (< 6 characters)")
    void changePassword_PasswordTooShort() throws Exception {
        ChangePasswordApiRequest request = ChangePasswordApiRequest.builder()
                .oldPassword("oldPassword123")
                .newPassword("12345")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1006"));

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/users: returns paged users successfully")
    void getAllUsers_Success() throws Exception {
        UserResult user1 = UserResult.builder()
                .userId(UUID.randomUUID())
                .email("user1@example.com")
                .fullName("User One")
                .build();
        UserResult user2 = UserResult.builder()
                .userId(UUID.randomUUID())
                .email("user2@example.com")
                .fullName("User Two")
                .build();

        PageResult<UserResult> pagedResult = PageResult.of(List.of(user1, user2), 0, 20, 2);
        when(adminUserService.getUsers(any())).thenReturn(pagedResult);
        when(userPresenter.toDto(any())).thenAnswer(inv -> {
            UserResult ur = inv.getArgument(0);
            return UserDto.builder().email(ur.getEmail()).build();
        });

        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$.content[1].email").value("user2@example.com"));

        verify(adminUserService).getUsers(any());
    }

    @Test
    @DisplayName("POST /api/v1/users/me/avatar: uploads avatar file and returns updated UserDto")
    void uploadAvatar_Success() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        when(userService.getById(authenticatedUser.getUserId())).thenReturn(java.util.Optional.of(
                UserResult.builder().userId(authenticatedUser.getUserId()).email(authenticatedUser.getEmail()).build()));
        when(userPresenter.toDto(any())).thenReturn(UserDto.builder()
                .userId(authenticatedUser.getUserId())
                .avatarUrl("https://res.cloudinary.com/example-cloud/image/upload/v1/avatar.png")
                .build());

        mockMvc.perform(multipart("/api/v1/users/me/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://res.cloudinary.com/example-cloud/image/upload/v1/avatar.png"));

        verify(userService).uploadAvatar(eq(authenticatedUser.getUserId()), any(), eq("image/png"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me/avatar: deletes avatar and returns updated UserDto")
    void deleteAvatar_Success() throws Exception {
        when(userService.getById(authenticatedUser.getUserId())).thenReturn(java.util.Optional.of(
                UserResult.builder().userId(authenticatedUser.getUserId()).email(authenticatedUser.getEmail()).build()));
        when(userPresenter.toDto(any())).thenReturn(UserDto.builder()
                .userId(authenticatedUser.getUserId())
                .avatarUrl(null)
                .build());

        mockMvc.perform(delete("/api/v1/users/me/avatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());

        verify(userService).deleteAvatar(authenticatedUser.getUserId());
    }
}
