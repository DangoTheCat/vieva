package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.EmailSenderPort;
import com.example.vieva.application.usecases.user.AccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails the new user after the admin create-user transaction committed. Runs on the application
 * task executor, so a slow or failing SMTP server never blocks or fails the admin request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCreatedEmailWorker {

    private static final String SUBJECT = "[VIEVA] Tài khoản của bạn đã được tạo";

    private final EmailSenderPort emailSender;

    @Value("${vieva.app.login-url:http://localhost:5173/login}")
    private String loginUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountCreated(AccountCreatedEvent event) {
        try {
            emailSender.send(event.email(), SUBJECT, buildBody(event));
            log.info("Account-created email handed to the mail provider for {}", event.email());
        } catch (Exception e) {
            // The account exists either way; the admin can reset the password to re-issue credentials
            log.error("Failed to send account-created email to {}", event.email(), e);
        }
    }

    private String buildBody(AccountCreatedEvent event) {
        return """
                Xin chào %s,

                Quản trị viên đã tạo tài khoản VIEVA cho bạn.

                Email đăng nhập: %s
                Vai trò: %s
                Mật khẩu tạm thời: %s

                Đăng nhập tại: %s
                Ở lần đăng nhập đầu tiên, hệ thống sẽ yêu cầu bạn đổi sang mật khẩu mới.

                Nếu bạn không yêu cầu tài khoản này, vui lòng liên hệ quản trị viên.
                """.formatted(event.fullName(), event.email(), displayRole(event.roleCode()),
                event.temporaryPassword(), loginUrl);
    }

    private String displayRole(String roleCode) {
        if (roleCode == null) {
            return "";
        }
        return roleCode.startsWith("ROLE_") ? roleCode.substring(5) : roleCode;
    }
}
