package com.example.vieva.integration;

import com.example.vieva.application.ports.input.CreateUserByAdminRequest;
import com.example.vieva.application.ports.input.UpdateUserByAdminRequest;
import com.example.vieva.application.ports.output.EmailSenderPort;
import com.example.vieva.application.usecases.user.AdminUserService;
import com.example.vieva.domain.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Admin account management against PostgreSQL with Flyway migrations: one role per account
 * (V13 deferred unique constraint), forced password change and the after-commit welcome email.
 * Requires Docker.
 */
@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "vieva.ai.provider=mock",
        "vieva.storage.provider=local"
})
@Testcontainers
class AdminUserIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    /** Seeded by V10. */
    private static final UUID ADMIN_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @Autowired private AdminUserService adminUserService;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private EmailSenderPort emailSender;

    @Test
    @DisplayName("V13 collapses seeded multi-role accounts to one role each")
    void seededAccountsHaveOneRole() {
        Integer multiRoleUsers = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (SELECT user_id FROM user_roles GROUP BY user_id HAVING COUNT(*) > 1) t",
                Integer.class);
        String adminRole = jdbc.queryForObject(
                "SELECT r.role_code FROM user_roles ur JOIN roles r ON r.role_id = ur.role_id WHERE ur.user_id = ?",
                String.class, ADMIN_ID);

        assertThat(multiRoleUsers).isZero();
        assertThat(adminRole).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Admin-created account: one role, must change password, emailed after commit; role change replaces it")
    void createThenChangeRole() {
        String email = "it-" + UUID.randomUUID().toString().substring(0, 8) + "@fpt.edu.vn";

        User created = adminUserService.createUser(CreateUserByAdminRequest.builder()
                .email(email)
                .fullName("Integration User")
                .roleCode("ROLE_STUDENT")
                .build(), ADMIN_ID);

        assertThat(jdbc.queryForObject("SELECT must_change_password FROM users WHERE user_id = ?",
                Boolean.class, created.getUserId())).isTrue();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(emailSender).send(eq(email), contains("Tài khoản"), contains("Mật khẩu tạm thời")));

        adminUserService.updateUser(created.getUserId(),
                UpdateUserByAdminRequest.builder().roleCode("LECTURER").build(), ADMIN_ID);

        assertThat(jdbc.queryForList(
                "SELECT r.role_code FROM user_roles ur JOIN roles r ON r.role_id = ur.role_id WHERE ur.user_id = ?",
                String.class, created.getUserId())).containsExactly("ROLE_LECTURER");
    }

    @Test
    @DisplayName("DB rejects a second role row for the same account")
    void secondRoleRowRejected() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, (SELECT role_id FROM roles WHERE role_code = 'ROLE_STUDENT'))",
                ADMIN_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
