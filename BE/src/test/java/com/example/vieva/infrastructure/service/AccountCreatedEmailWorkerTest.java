package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.EmailSenderPort;
import com.example.vieva.application.usecases.user.AccountCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCreatedEmailWorkerTest {

    @Mock
    private EmailSenderPort emailSender;

    private AccountCreatedEmailWorker worker;

    @BeforeEach
    void setUp() {
        worker = new AccountCreatedEmailWorker(emailSender);
        ReflectionTestUtils.setField(worker, "loginUrl", "http://localhost:5173/login");
    }

    @Test
    @DisplayName("Sends account created email with credentials, temporary password and instructions")
    void sendsAccountCreatedEmail() {
        AccountCreatedEvent event = new AccountCreatedEvent(
                "student@fpt.edu.vn",
                "Nguyen Van A",
                "ROLE_STUDENT",
                "TempPass123!"
        );

        worker.onAccountCreated(event);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailSender).send(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("student@fpt.edu.vn");
        assertThat(subjectCaptor.getValue()).contains("[VIEVA] Tài khoản của bạn đã được tạo");
        assertThat(bodyCaptor.getValue())
                .contains("Xin chào Nguyen Van A")
                .contains("student@fpt.edu.vn")
                .contains("STUDENT")
                .contains("TempPass123!")
                .contains("http://localhost:5173/login")
                .contains("đổi sang mật khẩu mới");
    }

    @Test
    @DisplayName("Swallows mail sender exceptions to prevent async worker failure")
    void swallowsMailSenderException() {
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        AccountCreatedEvent event = new AccountCreatedEvent(
                "lecturer@fpt.edu.vn",
                "Tran Thi B",
                "ROLE_LECTURER",
                "LecturerPass999#"
        );

        assertThatCode(() -> worker.onAccountCreated(event)).doesNotThrowAnyException();
        verify(emailSender).send(eq("lecturer@fpt.edu.vn"), anyString(), anyString());
    }
}
